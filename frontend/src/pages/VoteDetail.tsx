import React, { useCallback, useEffect, useState } from 'react'
import {
  App as AntdApp,
  Alert,
  Button,
  Card,
  Checkbox,
  Col,
  Descriptions,
  Input,
  InputNumber,
  Progress,
  Radio,
  Row,
  Slider,
  Space,
  Spin,
  Switch,
  Tag,
  Typography,
} from 'antd'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import http from '../api/http'
import { getApiErrorMessage, hasStoredToken } from '../lib/auth'
import { getEnvelopeErrorMessage } from '../lib/errors'

const { Title, Paragraph, Text } = Typography

interface Option {
  id: number
  text: string
  votes: number
  maxScore: number
  averageScore: number
}

interface VoteDetailData {
  id: number
  title: string
  description: string
  options: Option[]
  totalVotes: number
  participants: number
  hasVoted: boolean
  status: string
  type: string
  minChoices: number
  maxChoices: number
  forceAllOptions: boolean
  allowCustomOptions: boolean
}

interface VoteInviteData {
  voteId: number
  accessType: string
  enabled: boolean
  codeVersion?: number | null
  code?: string | null
  codeMasked?: string | null
  expiresAt?: string | null
  maxMembers?: number | null
  activeMembers?: number
  canViewPlainCode: boolean
}

function formatDate(value?: string | null) {
  if (!value) {
    return '未设置'
  }

  return new Date(value).toLocaleString('zh-CN')
}

function toLocalDateTimeInputValue(value?: string | null) {
  if (!value) {
    return ''
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return ''
  }

  const offset = date.getTimezoneOffset() * 60 * 1000
  return new Date(date.getTime() - offset).toISOString().slice(0, 16)
}

async function copyText(
  text: string,
  successMessage: string,
  messageApi: ReturnType<typeof AntdApp.useApp>['message'],
) {
  if (!text) {
    messageApi.warning('暂无可复制内容')
    return
  }

  try {
    await navigator.clipboard.writeText(text)
    messageApi.success(successMessage)
  } catch {
    messageApi.error('复制失败，请手动复制')
  }
}

const VoteDetail: React.FC = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const { message } = AntdApp.useApp()
  const [searchParams] = useSearchParams()
  const [selectedOption, setSelectedOption] = useState<number | null>(null)
  const [selectedOptions, setSelectedOptions] = useState<number[]>([])
  const [optionScores, setOptionScores] = useState<Record<number, number>>({})
  const [voting, setVoting] = useState(false)
  const [loading, setLoading] = useState(true)
  const [voteData, setVoteData] = useState<VoteDetailData | null>(null)
  const [newOptionText, setNewOptionText] = useState('')
  const [addingOption, setAddingOption] = useState(false)
  const [accessDenied, setAccessDenied] = useState(false)
  const [voteNotFound, setVoteNotFound] = useState(false)
  const [joining, setJoining] = useState(false)
  const [inviteCode, setInviteCode] = useState('')
  const [inviteInfo, setInviteInfo] = useState<VoteInviteData | null>(null)
  const [inviteLoading, setInviteLoading] = useState(false)
  const [savingInvite, setSavingInvite] = useState(false)
  const [resettingInvite, setResettingInvite] = useState(false)
  const [inviteEnabled, setInviteEnabled] = useState(false)
  const [inviteMaxMembers, setInviteMaxMembers] = useState<number>(100)
  const [inviteExpiresAt, setInviteExpiresAt] = useState('')
  const [prefilledInviteCode, setPrefilledInviteCode] = useState('')
  const isLoggedIn = hasStoredToken()
  const inviteFromLink = searchParams.get('invite')?.trim() || ''
  const currentRelativeUrl = `${window.location.pathname}${window.location.search}`
  const loginRedirectUrl = `/login?redirect=${encodeURIComponent(currentRelativeUrl)}`
  const registerRedirectUrl = `/register?redirect=${encodeURIComponent(currentRelativeUrl)}`
  const managerInviteLink = inviteInfo?.code ? `${window.location.origin}/vote/${id}?invite=${inviteInfo.code}` : ''

  useEffect(() => {
    if (inviteFromLink && inviteFromLink !== prefilledInviteCode) {
      setInviteCode(inviteFromLink)
      setPrefilledInviteCode(inviteFromLink)
    }
  }, [inviteFromLink, prefilledInviteCode])

  useEffect(() => {
    if (!inviteInfo) {
      return
    }

    setInviteEnabled(inviteInfo.accessType === 'INVITE' || inviteInfo.enabled)
    setInviteMaxMembers(inviteInfo.maxMembers || 100)
    setInviteExpiresAt(toLocalDateTimeInputValue(inviteInfo.expiresAt))
  }, [inviteInfo])

  const fetchInviteInfo = useCallback(async (silent = false) => {
    if (!id || !isLoggedIn) {
      setInviteInfo(null)
      return
    }

    if (!silent) {
      setInviteLoading(true)
    }

    try {
      const res = await http.get(`/api/votes/${id}/invite`)
      if (res.data?.success) {
        setInviteInfo(res.data.data)
      } else if (!silent) {
        message.error(getEnvelopeErrorMessage(res.data?.message, '获取邀请码信息失败'))
      }
    } catch (error) {
      const status = (error as { response?: { status?: number } })?.response?.status
      if (status === 401 || status === 403) {
        setInviteInfo(null)
      } else if (!silent) {
        message.error(getApiErrorMessage(error, '获取邀请码信息失败'))
      }
    } finally {
      if (!silent) {
        setInviteLoading(false)
      }
    }
  }, [id, isLoggedIn])

  const fetchVoteDetail = useCallback(async () => {
    if (!id) {
      setLoading(false)
      return
    }

    setLoading(true)
    try {
      const res = await http.get(`/api/votes/${id}/results`)
      if (res.data?.success) {
        setVoteData(res.data.data)
        setAccessDenied(false)
        setVoteNotFound(false)
        await fetchInviteInfo(true)
      } else {
        message.error(getEnvelopeErrorMessage(res.data?.message, '获取详情失败'))
      }
    } catch (error) {
      const status = (error as { response?: { status?: number } })?.response?.status
      if (status === 403) {
        setAccessDenied(true)
        setVoteData(null)
        setVoteNotFound(false)
      } else if (status === 404) {
        setVoteNotFound(true)
        setVoteData(null)
        setAccessDenied(false)
      } else {
        message.error(getApiErrorMessage(error, '获取详情失败'))
      }
    } finally {
      setLoading(false)
    }
  }, [fetchInviteInfo, id])

  useEffect(() => {
    void fetchVoteDetail()
  }, [fetchVoteDetail])

  const handleJoinVote = async () => {
    if (!id) {
      return
    }

    if (!isLoggedIn) {
      navigate(loginRedirectUrl)
      return
    }

    if (!inviteCode.trim()) {
      message.warning('请输入邀请码')
      return
    }

    setJoining(true)
    try {
      const res = await http.post(`/api/votes/${id}/join`, {
        inviteCode: inviteCode.trim(),
      })

      if (res.data?.success) {
        message.success('加入成功')
        setInviteCode('')
        await fetchVoteDetail()
      } else {
        message.error(getEnvelopeErrorMessage(res.data?.message, '加入投票失败'))
      }
    } catch (error) {
      message.error(getApiErrorMessage(error, '加入投票失败'))
    } finally {
      setJoining(false)
    }
  }

  const handleSaveInviteSettings = async () => {
    if (!id) {
      return
    }

    setSavingInvite(true)
    try {
      const payload: Record<string, unknown> = {
        accessType: inviteEnabled ? 'INVITE' : 'PUBLIC',
        enabled: inviteEnabled,
        maxMembers: inviteMaxMembers,
      }

      if (inviteExpiresAt) {
        payload.expiresAt = new Date(inviteExpiresAt).toISOString()
      }

      const res = await http.patch(`/api/votes/${id}/invite/settings`, payload)
      if (res.data?.success) {
        message.success('邀请码设置已更新')
        setInviteInfo(res.data.data)
      } else {
        message.error(getEnvelopeErrorMessage(res.data?.message, '保存邀请码设置失败'))
      }
    } catch (error) {
      message.error(getApiErrorMessage(error, '保存邀请码设置失败'))
    } finally {
      setSavingInvite(false)
    }
  }

  const handleResetInviteCode = async () => {
    if (!id) {
      return
    }

    setResettingInvite(true)
    try {
      const res = await http.post(`/api/votes/${id}/invite/reset`)
      if (res.data?.success) {
        message.success('邀请码已重置')
        setInviteInfo(res.data.data)
      } else {
        message.error(getEnvelopeErrorMessage(res.data?.message, '重置邀请码失败'))
      }
    } catch (error) {
      message.error(getApiErrorMessage(error, '重置邀请码失败'))
    } finally {
      setResettingInvite(false)
    }
  }

  const handleVote = async () => {
    if (!id || !voteData) {
      return
    }

    let payload: Record<string, unknown> = {}

    if (voteData.type === 'CHOICE') {
      const choices = voteData.maxChoices === 1 ? (selectedOption ? [selectedOption] : []) : selectedOptions
      if (choices.length < voteData.minChoices || choices.length > voteData.maxChoices) {
        message.warning(`请选择 ${voteData.minChoices} 到 ${voteData.maxChoices} 个选项`)
        return
      }
      payload = { optionIds: choices }
    } else if (voteData.type === 'SLIDER') {
      if (voteData.forceAllOptions && Object.keys(optionScores).length !== voteData.options.length) {
        message.warning('请为所有选项评分')
        return
      }
      if (Object.keys(optionScores).length === 0) {
        message.warning('请至少为一个选项评分')
        return
      }
      payload = { optionScores }
    }

    setVoting(true)
    try {
      const res = await http.post(`/api/votes/${id}/vote`, payload)
      if (res.data?.success) {
        message.success('投票成功')
        await fetchVoteDetail()
      } else {
        message.error(getEnvelopeErrorMessage(res.data?.message, '投票失败'))
      }
    } catch (error) {
      message.error(getApiErrorMessage(error, '投票失败，请稍后重试'))
    } finally {
      setVoting(false)
    }
  }

  const handleAddOption = async () => {
    if (!id || !voteData) {
      return
    }

    if (!newOptionText.trim()) {
      message.warning('请输入选项内容')
      return
    }

    setAddingOption(true)
    try {
      const payload: Record<string, unknown> = { text: newOptionText.trim() }
      if (voteData.type === 'SLIDER') {
        payload.maxScore = 100
      }

      const res = await http.post(`/api/votes/${id}/options`, payload)
      if (res.data?.success) {
        message.success('选项添加成功')
        setNewOptionText('')
        await fetchVoteDetail()
      } else {
        message.error(getEnvelopeErrorMessage(res.data?.message, '选项添加失败'))
      }
    } catch (error) {
      message.error(getApiErrorMessage(error, '选项添加失败，请重试'))
    } finally {
      setAddingOption(false)
    }
  }

  if (loading) {
    return (
      <div className="p-12 text-center">
        <Spin size="large" />
      </div>
    )
  }

  if (voteNotFound) {
    return <div className="p-12 text-center">未找到该投票信息</div>
  }

  if (accessDenied) {
    return (
      <div className="mx-auto max-w-2xl">
        <Button type="link" onClick={() => navigate('/')} className="mb-4">
          &larr; 返回列表
        </Button>
        <Card>
          <Title level={3}>该投票需要邀请码加入</Title>
          <Paragraph className="text-gray-500">
            当前账号尚未具备该投票的访问资格。登录后输入邀请码即可加入，加入成功后会自动恢复详情访问。
          </Paragraph>
          {inviteFromLink ? (
            <Alert
              type="info"
              showIcon
              className="mb-4"
              title="已自动填入邀请信息"
              description="确认登录后可直接点击“加入投票”。"
            />
          ) : null}

          {!isLoggedIn ? (
            <Alert
              type="info"
              showIcon
              title="请先登录"
              description={inviteFromLink ? '登录后会返回当前页面，可直接继续加入。' : '登录后输入邀请码即可加入该投票。'}
              action={
                <Space>
                  <Button type="primary" onClick={() => navigate(loginRedirectUrl)}>
                    去登录
                  </Button>
                  <Button onClick={() => navigate(registerRedirectUrl)}>
                    去注册
                  </Button>
                </Space>
              }
            />
          ) : (
            <div className="space-y-4">
              <Space.Compact className="w-full">
                <Input
                  placeholder="请输入邀请码"
                  value={inviteCode}
                  onChange={(event) => setInviteCode(event.target.value)}
                  onPressEnter={handleJoinVote}
                  maxLength={32}
                />
                <Button type="primary" loading={joining} onClick={handleJoinVote}>
                  加入投票
                </Button>
              </Space.Compact>
              <div className="flex items-center justify-between text-sm text-gray-500">
                <span>{inviteFromLink ? '已从邀请链接带入邀请码，可直接加入。' : '没有邀请码时可向发起人索取。'}</span>
                {inviteFromLink ? (
                  <Button type="link" onClick={() => setInviteCode('')} className="!px-0">
                    清空重填
                  </Button>
                ) : null}
              </div>
            </div>
          )}
        </Card>
      </div>
    )
  }

  if (!voteData) {
    return <div className="p-12 text-center">未找到该投票信息</div>
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <Button type="link" onClick={() => navigate('/')} className="mb-0">
        &larr; 返回列表
      </Button>

      {inviteInfo && (inviteInfo.accessType === 'INVITE' || inviteInfo.enabled) ? (
        <Card
          title="邀请码管理"
          loading={inviteLoading}
          extra={<Tag color="volcano">创建者或管理员可见</Tag>}
        >
          <Descriptions column={1} size="small" className="mb-4">
            <Descriptions.Item label="访问模式">
              {inviteInfo.accessType === 'INVITE' ? '邀请码访问' : '公开访问'}
            </Descriptions.Item>
            <Descriptions.Item label="邀请码">
              {inviteInfo.code || inviteInfo.codeMasked || '尚未生成'}
            </Descriptions.Item>
            <Descriptions.Item label="邀请链接">
              {managerInviteLink ? (
                <Typography.Paragraph copyable={{ text: managerInviteLink }} className="!mb-0">
                  {managerInviteLink}
                </Typography.Paragraph>
              ) : (
                '尚未生成'
              )}
            </Descriptions.Item>
            <Descriptions.Item label="活跃成员">
              {inviteInfo.activeMembers ?? 0} / {inviteInfo.maxMembers ?? '未限制'}
            </Descriptions.Item>
            <Descriptions.Item label="过期时间">
              {formatDate(inviteInfo.expiresAt)}
            </Descriptions.Item>
            <Descriptions.Item label="版本号">
              {inviteInfo.codeVersion || 1}
            </Descriptions.Item>
          </Descriptions>

          <div className="grid gap-4 md:grid-cols-3">
            <div>
              <Text className="mb-2 block">启用邀请码访问</Text>
              <Switch checked={inviteEnabled} onChange={setInviteEnabled} />
            </div>
            <div>
              <Text className="mb-2 block">成员上限</Text>
              <InputNumber
                min={1}
                className="w-full"
                value={inviteMaxMembers}
                onChange={(value) => setInviteMaxMembers(value || 1)}
              />
            </div>
            <div>
              <Text className="mb-2 block">过期时间</Text>
              <Input
                type="datetime-local"
                value={inviteExpiresAt}
                onChange={(event) => setInviteExpiresAt(event.target.value)}
              />
            </div>
          </div>

          <Space className="mt-4" wrap>
            <Button type="primary" loading={savingInvite} onClick={handleSaveInviteSettings}>
              保存设置
            </Button>
            <Button loading={resettingInvite} onClick={handleResetInviteCode}>
              重置邀请码
            </Button>
            {inviteInfo.code ? (
              <Button onClick={() => void copyText(inviteInfo.code || '', '邀请码已复制', message)}>
                复制邀请码
              </Button>
            ) : null}
            {managerInviteLink ? (
              <Button onClick={() => void copyText(managerInviteLink, '邀请链接已复制', message)}>
                复制邀请链接
              </Button>
            ) : null}
            <Text type="secondary">留空过期时间时会保留当前后端已有值。</Text>
          </Space>
        </Card>
      ) : null}

      <Card>
        <Title level={3}>{voteData.title}</Title>
        <Paragraph className="text-gray-500">{voteData.description}</Paragraph>
        {inviteFromLink ? (
          <Alert
            type="info"
            showIcon
            className="mb-4"
            title="你是通过邀请链接进入的"
            description="当前页面已识别邀请信息；如果该投票需要加入权限，可在受限提示或下方管理区继续使用。"
          />
        ) : null}

        <div className="mt-8">
          {!voteData.hasVoted && voteData.status === 'active' ? (
            <div className="space-y-4">
              {voteData.type === 'CHOICE' ? (
                voteData.maxChoices === 1 ? (
                  <Radio.Group
                    className="flex w-full flex-col space-y-4"
                    onChange={(event) => setSelectedOption(event.target.value)}
                    value={selectedOption}
                  >
                    {voteData.options.map((opt) => (
                      <Radio
                        key={opt.id}
                        value={opt.id}
                        className="w-full rounded-lg border p-4 text-lg transition-colors hover:bg-gray-50"
                      >
                        {opt.text}
                      </Radio>
                    ))}
                  </Radio.Group>
                ) : (
                  <Checkbox.Group
                    className="flex w-full flex-col space-y-4"
                    onChange={(checkedValues) => setSelectedOptions(checkedValues as number[])}
                    value={selectedOptions}
                  >
                    {voteData.options.map((opt) => (
                      <Checkbox
                        key={opt.id}
                        value={opt.id}
                        className="m-0 w-full rounded-lg border p-4 text-lg transition-colors hover:bg-gray-50"
                      >
                        {opt.text}
                      </Checkbox>
                    ))}
                  </Checkbox.Group>
                )
              ) : (
                <div className="space-y-6">
                  {voteData.options.map((opt) => (
                    <div key={opt.id} className="rounded-lg border bg-gray-50 p-4">
                      <div className="mb-2 flex justify-between">
                        <span className="font-semibold">{opt.text}</span>
                        <span className="text-sm text-gray-500">满分: {opt.maxScore}</span>
                      </div>
                      <Row gutter={16} align="middle">
                        <Col span={18}>
                          <Slider
                            min={0}
                            max={opt.maxScore}
                            onChange={(value) =>
                              setOptionScores({
                                ...optionScores,
                                [opt.id]: value,
                              })
                            }
                            value={typeof optionScores[opt.id] === 'number' ? optionScores[opt.id] : 0}
                          />
                        </Col>
                        <Col span={6}>
                          <InputNumber
                            min={0}
                            max={opt.maxScore}
                            style={{ margin: '0 16px' }}
                            value={typeof optionScores[opt.id] === 'number' ? optionScores[opt.id] : 0}
                            onChange={(value) =>
                              setOptionScores({
                                ...optionScores,
                                [opt.id]: value || 0,
                              })
                            }
                          />
                        </Col>
                      </Row>
                    </div>
                  ))}
                </div>
              )}

              {voteData.allowCustomOptions ? (
                <div className="mt-4 rounded-lg border border-blue-100 bg-blue-50 p-4">
                  <div className="mb-2 text-sm font-semibold text-blue-800">
                    找不到想要的选项？自己添加一个：
                  </div>
                  <div className="flex gap-2">
                    <Input
                      placeholder="输入新选项内容..."
                      value={newOptionText}
                      onChange={(event) => setNewOptionText(event.target.value)}
                      onPressEnter={handleAddOption}
                      maxLength={100}
                    />
                    <Button type="primary" onClick={handleAddOption} loading={addingOption}>
                      添加
                    </Button>
                  </div>
                </div>
              ) : null}

              <Button
                type="primary"
                size="large"
                className="mt-8 w-full"
                onClick={handleVote}
                loading={voting}
              >
                提交投票
              </Button>
            </div>
          ) : (
            <div className="mt-6 space-y-6">
              <Title level={4}>投票结果 (参与人数: {voteData.participants})</Title>
              {voteData.options.map((opt) => {
                if (voteData.type === 'CHOICE') {
                  const percent = voteData.totalVotes === 0 ? 0 : Math.round((opt.votes / voteData.totalVotes) * 100)
                  return (
                    <div key={opt.id}>
                      <div className="mb-1 flex justify-between">
                        <span>{opt.text}</span>
                        <span className="text-gray-500">
                          {opt.votes} 票 ({percent}%)
                        </span>
                      </div>
                      <Progress percent={percent} showInfo={false} status="active" />
                    </div>
                  )
                }

                const percent = opt.maxScore === 0 ? 0 : Math.round((opt.averageScore / opt.maxScore) * 100)
                return (
                  <div key={opt.id} className="mb-4">
                    <div className="mb-1 flex justify-between">
                      <span>{opt.text}</span>
                      <span className="text-gray-500">
                        平均分: {opt.averageScore.toFixed(1)} / {opt.maxScore}
                      </span>
                    </div>
                    <Progress
                      percent={percent}
                      showInfo={false}
                      status="active"
                      format={() => `${opt.averageScore.toFixed(1)}分`}
                    />
                    <div className="mt-1 text-xs text-gray-400">评分人数: {opt.votes}</div>
                  </div>
                )
              })}
              <div className="mt-8 text-center font-semibold text-green-600">
                {voteData.hasVoted ? '您已完成投票，感谢参与！' : '投票已结束'}
              </div>
            </div>
          )}
        </div>
      </Card>
    </div>
  )
}

export default VoteDetail
