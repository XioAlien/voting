import React, { useState } from 'react'
import {
  App as AntdApp,
  Alert,
  Button,
  Card,
  Col,
  DatePicker,
  Descriptions,
  Divider,
  Form,
  Input,
  InputNumber,
  Result,
  Radio,
  Row,
  Space,
  Spin,
  Statistic,
  Switch,
  Tag,
  Typography,
} from 'antd'
import { MinusCircleOutlined, PlusOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import http from '../api/http'
import UserManagementPanel from '../components/UserManagementPanel'
import { getApiErrorMessage, hasStoredToken } from '../lib/auth'
import { getEnvelopeErrorMessage, getUserFacingErrorMessage } from '../lib/errors'

const { Title, Paragraph } = Typography
const { RangePicker } = DatePicker
const { TextArea } = Input

interface AdminPageProps {
  showAdminModules?: boolean
}

interface AdminDashboardData {
  totalUsers: number
  activeUsers: number
  totalVotes: number
  activeVotes: number
  totalVoteRecords: number
  totalAuditLogs: number
}

interface VoteFormOption {
  text: string
  maxScore?: number
}

interface CreateVoteFormValues {
  title?: string
  description?: string
  type: 'CHOICE' | 'SLIDER'
  accessType: 'PUBLIC' | 'INVITE'
  minChoices?: number
  maxChoices?: number
  forceAllOptions?: boolean
  allowCustomOptions?: boolean
  inviteMaxMembers?: number
  inviteExpiresAt?: string
  timeRange?: Array<{ toISOString: () => string }>
  options: VoteFormOption[]
}

interface CreatedVoteInviteData {
  voteId: number
  accessType: string
  enabled: boolean
  code?: string | null
  codeMasked?: string | null
  expiresAt?: string | null
  maxMembers?: number | null
  activeMembers?: number
  canViewPlainCode: boolean
}

interface CreatedVoteResult {
  vote: {
    id: number
    title: string
    description?: string
    status: string
    type: string
    participants: number
    startTime?: string
    endTime?: string
  }
  accessType: 'PUBLIC' | 'INVITE'
  invite?: CreatedVoteInviteData | null
}

function buildDefaultInviteExpiryInputValue() {
  const date = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000)
  const offset = date.getTimezoneOffset() * 60 * 1000
  return new Date(date.getTime() - offset).toISOString().slice(0, 16)
}

function buildInitialFormValues(): CreateVoteFormValues {
  return {
    type: 'CHOICE',
    accessType: 'PUBLIC',
    minChoices: 1,
    maxChoices: 1,
    forceAllOptions: false,
    allowCustomOptions: false,
    inviteMaxMembers: 100,
    inviteExpiresAt: buildDefaultInviteExpiryInputValue(),
    options: [
      { text: '选项1', maxScore: 100 },
      { text: '选项2', maxScore: 100 },
    ],
  }
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return '未设置'
  }

  return new Date(value).toLocaleString('zh-CN')
}

function buildInviteLink(voteId: number, inviteCode?: string | null) {
  if (!inviteCode) {
    return ''
  }

  const url = new URL(`/vote/${voteId}`, window.location.origin)
  url.searchParams.set('invite', inviteCode)
  return url.toString()
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

const AdminOverview: React.FC = () => {
  const { message } = AntdApp.useApp()
  const [dashboard, setDashboard] = React.useState<AdminDashboardData | null>(null)
  const [loading, setLoading] = React.useState(true)

  const fetchDashboard = React.useCallback(async () => {
    setLoading(true)
    try {
      const res = await http.get('/api/admin/dashboard')
      if (res.data?.success) {
        setDashboard(res.data.data)
      } else {
        message.error(getEnvelopeErrorMessage(res.data?.message, '获取管理员概览失败'))
      }
    } catch (error) {
      message.error(getUserFacingErrorMessage(error, '获取管理员概览失败'))
    } finally {
      setLoading(false)
    }
  }, [])

  React.useEffect(() => {
    void fetchDashboard()
  }, [fetchDashboard])

  if (loading) {
    return (
      <Card className="mb-6">
        <div className="text-center py-8">
          <Spin />
        </div>
      </Card>
    )
  }

  if (!dashboard) {
    return (
      <Alert
        className="mb-6"
        type="warning"
        showIcon
        title="管理员概览暂时不可用"
        description="用户管理仍可继续访问，稍后可点击刷新页面重新拉取统计信息。"
      />
    )
  }

  return (
    <Card className="mb-6" title="管理员概览">
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={8}>
          <Statistic title="用户总数" value={dashboard.totalUsers} />
        </Col>
        <Col xs={24} sm={12} lg={8}>
          <Statistic title="活跃用户" value={dashboard.activeUsers} />
        </Col>
        <Col xs={24} sm={12} lg={8}>
          <Statistic title="投票总数" value={dashboard.totalVotes} />
        </Col>
        <Col xs={24} sm={12} lg={8}>
          <Statistic title="进行中投票" value={dashboard.activeVotes} />
        </Col>
        <Col xs={24} sm={12} lg={8}>
          <Statistic title="投票记录" value={dashboard.totalVoteRecords} />
        </Col>
        <Col xs={24} sm={12} lg={8}>
          <Statistic title="审计日志" value={dashboard.totalAuditLogs} />
        </Col>
      </Row>
    </Card>
  )
}

const Admin: React.FC<AdminPageProps> = ({ showAdminModules = false }) => {
  const [form] = Form.useForm()
  const navigate = useNavigate()
  const { message } = AntdApp.useApp()
  const [loading, setLoading] = useState(false)
  const [voteType, setVoteType] = useState('CHOICE')
  const [accessType, setAccessType] = useState<'PUBLIC' | 'INVITE'>('PUBLIC')
  const [createdResult, setCreatedResult] = useState<CreatedVoteResult | null>(null)
  const isLoggedIn = hasStoredToken()
  const initialFormValues = React.useMemo(() => buildInitialFormValues(), [])
  const inviteLink = createdResult?.invite?.code
    ? buildInviteLink(createdResult.vote.id, createdResult.invite.code)
    : ''
  const loginRedirectUrl = `/login?redirect=${encodeURIComponent('/create')}`

  const onFinish = async (values: CreateVoteFormValues) => {
    if (!values.options || values.options.length < 2) {
      message.error('至少需要提供2个选项')
      return
    }

    setLoading(true)
    try {
      const start = values.timeRange?.[0]?.toISOString()
      const end = values.timeRange?.[1]?.toISOString()

      const payload = {
        title: values.title,
        description: values.description,
        startTime: start,
        endTime: end,
        type: values.type,
        accessType: values.accessType,
        minChoices: values.minChoices || 1,
        maxChoices: values.maxChoices || 1,
        forceAllOptions: values.forceAllOptions || false,
        allowCustomOptions: values.allowCustomOptions || false,
        inviteMaxMembers: values.accessType === 'INVITE' ? values.inviteMaxMembers || 100 : undefined,
        inviteExpiresAt:
          values.accessType === 'INVITE' && values.inviteExpiresAt
            ? new Date(values.inviteExpiresAt).toISOString()
            : undefined,
        options: values.options.map((opt) => ({
          text: opt.text,
          maxScore: opt.maxScore || 100,
        })),
      }

      const res = await http.post('/api/votes', payload)
      if (res.data?.success) {
        message.success('投票创建成功')
        setCreatedResult(res.data.data)
        form.resetFields()
        form.setFieldsValue(initialFormValues)
        setVoteType('CHOICE')
        setAccessType('PUBLIC')
      } else {
        message.error(getEnvelopeErrorMessage(res.data?.message, '创建失败'))
      }
    } catch (error) {
      message.error(getApiErrorMessage(error, '创建失败，请重试'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-5xl mx-auto space-y-6">
      <div>
        <Title level={2} className="!mb-2">
          创建投票
        </Title>
        <Paragraph className="!mb-0 text-gray-500">
          填写投票信息并发布，让参与者可以开始投票。
        </Paragraph>
      </div>

      {showAdminModules ? (
        <>
          <AdminOverview />
          <UserManagementPanel />
        </>
      ) : null}

      {createdResult ? (
        <Card className="shadow-sm">
          <Result
            status="success"
            title="投票已创建"
            subTitle={
              createdResult.accessType === 'INVITE'
                ? '邀请码和邀请链接已经准备好，可以直接分享。'
                : '公开投票已创建，参与者可以直接访问。'
            }
            extra={[
              <Button key="detail" type="primary" onClick={() => navigate(`/vote/${createdResult.vote.id}`)}>
                查看投票
              </Button>,
              createdResult.accessType === 'INVITE' && createdResult.invite?.code ? (
                <Button
                  key="copy-code"
                  onClick={() => void copyText(createdResult.invite?.code || '', '邀请码已复制', message)}
                >
                  复制邀请码
                </Button>
              ) : null,
              createdResult.accessType === 'INVITE' && inviteLink ? (
                <Button key="copy-link" onClick={() => void copyText(inviteLink, '邀请链接已复制', message)}>
                  复制邀请链接
                </Button>
              ) : null,
              <Button
                key="create-another"
                onClick={() => {
                  setCreatedResult(null)
                  form.setFieldsValue(buildInitialFormValues())
                  setVoteType('CHOICE')
                  setAccessType('PUBLIC')
                }}
              >
                继续创建
              </Button>,
            ].filter(Boolean)}
          />

          {createdResult.accessType === 'INVITE' ? (
            <Alert
              type="success"
              showIcon
              className="mb-4"
              title="下一步建议"
              description="先复制邀请链接分享给参与者；参与者打开链接后会直达投票页，并看到已预填的邀请码。"
            />
          ) : (
            <Alert
              type="info"
              showIcon
              className="mb-4"
              title="公开投票已就绪"
              description="你可以直接分享投票地址，参与者打开后即可查看并投票。"
            />
          )}

          <Descriptions column={1} bordered size="middle">
            <Descriptions.Item label="投票标题">{createdResult.vote.title}</Descriptions.Item>
            <Descriptions.Item label="访问方式">
              <Tag color={createdResult.accessType === 'INVITE' ? 'purple' : 'blue'}>
                {createdResult.accessType === 'INVITE' ? '邀请码投票' : '公开投票'}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="投票地址">
              <Typography.Paragraph
                copyable={{ text: `${window.location.origin}/vote/${createdResult.vote.id}` }}
                className="!mb-0"
              >
                {`${window.location.origin}/vote/${createdResult.vote.id}`}
              </Typography.Paragraph>
            </Descriptions.Item>
            <Descriptions.Item label="投票状态">
              <Tag color={createdResult.vote.status === 'active' ? 'green' : 'default'}>
                {createdResult.vote.status === 'active' ? '进行中' : '已结束'}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="开始时间">
              {formatDateTime(createdResult.vote.startTime)}
            </Descriptions.Item>
            <Descriptions.Item label="结束时间">
              {formatDateTime(createdResult.vote.endTime)}
            </Descriptions.Item>
            {createdResult.accessType === 'INVITE' && createdResult.invite?.code ? (
              <>
                <Descriptions.Item label="邀请码">
                  <Typography.Paragraph
                    copyable={{ text: createdResult.invite.code }}
                    className="!mb-0"
                  >
                    {createdResult.invite.code}
                  </Typography.Paragraph>
                </Descriptions.Item>
                <Descriptions.Item label="邀请链接">
                  <Typography.Paragraph
                    copyable={{ text: inviteLink }}
                    className="!mb-0"
                  >
                    {inviteLink}
                  </Typography.Paragraph>
                </Descriptions.Item>
                <Descriptions.Item label="邀请码有效期">
                  {formatDateTime(createdResult.invite.expiresAt)}
                </Descriptions.Item>
                <Descriptions.Item label="成员上限">
                  {createdResult.invite.maxMembers || 100}
                </Descriptions.Item>
              </>
            ) : null}
          </Descriptions>
        </Card>
      ) : null}

      {!isLoggedIn ? (
        <Card>
          <Alert
            type="warning"
            showIcon
            title="创建投票需要先登录"
            description="登录后即可创建投票。"
            action={
              <Button type="primary" onClick={() => navigate(loginRedirectUrl)}>
                去登录
              </Button>
            }
          />
        </Card>
      ) : null}

      {!createdResult ? (
        <Card className="shadow-sm" title="创建投票">
        <Form
          form={form}
          layout="vertical"
          name="createVote"
          onFinish={onFinish}
          initialValues={initialFormValues}
          onValuesChange={(changedValues) => {
            if (changedValues.type) {
              setVoteType(changedValues.type)
            }
            if (changedValues.accessType) {
              setAccessType(changedValues.accessType)
            }
          }}
        >
          <Form.Item
            name="title"
            label="投票标题"
            rules={[{ required: true, message: '请输入标题' }]}
          >
            <Input placeholder="请输入引人注目的标题" size="large" />
          </Form.Item>

          <Form.Item name="description" label="详细描述">
            <TextArea rows={4} placeholder="选填，说明投票目的或注意事项" />
          </Form.Item>

          <Form.Item name="timeRange" label="有效时间 (选填)">
            <RangePicker showTime className="w-full" size="large" />
          </Form.Item>

          <Divider>设置投票规则</Divider>

          <Form.Item name="type" label="投票类型">
            <Radio.Group optionType="button" buttonStyle="solid">
              <Radio.Button value="CHOICE">传统选择模式</Radio.Button>
              <Radio.Button value="SLIDER">滑块评分模式</Radio.Button>
            </Radio.Group>
          </Form.Item>

          {voteType === 'CHOICE' ? (
            <Row gutter={16}>
              <Col span={12}>
                <Form.Item name="minChoices" label="最少选择数量">
                  <InputNumber min={1} className="w-full" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item name="maxChoices" label="最多选择数量">
                  <InputNumber min={1} className="w-full" />
                </Form.Item>
              </Col>
            </Row>
          ) : (
            <Form.Item name="forceAllOptions" valuePropName="checked">
              <Switch
                checkedChildren="强制必须评完所有选项"
                unCheckedChildren="不强制必须评完所有选项"
              />
            </Form.Item>
          )}

          <Divider>访问方式</Divider>

          <Form.Item name="accessType" label="参与方式">
            <Radio.Group optionType="button" buttonStyle="solid">
              <Radio.Button value="PUBLIC">公开投票</Radio.Button>
              <Radio.Button value="INVITE">邀请码投票</Radio.Button>
            </Radio.Group>
          </Form.Item>

          {accessType === 'INVITE' ? (
            <>
              <Alert
                type="info"
                showIcon
                className="mb-4"
                title="创建后会自动生成邀请码和邀请链接"
                description="参与者打开邀请链接后会直达该投票，并自动填入邀请码。"
              />
              <Row gutter={16}>
                <Col xs={24} md={12}>
                  <Form.Item
                    name="inviteMaxMembers"
                    label="成员上限"
                    rules={[{ required: true, message: '请输入成员上限' }]}
                  >
                    <InputNumber min={1} max={100000} className="w-full" />
                  </Form.Item>
                </Col>
                <Col xs={24} md={12}>
                  <Form.Item
                    name="inviteExpiresAt"
                    label="邀请码有效期"
                    rules={[{ required: true, message: '请选择邀请码有效期' }]}
                  >
                    <Input type="datetime-local" size="large" />
                  </Form.Item>
                </Col>
              </Row>
            </>
          ) : null}

          <Divider>设置投票选项</Divider>

          <Form.Item name="allowCustomOptions" valuePropName="checked" className="mb-4">
            <Switch
              checkedChildren="允许参与者添加选项"
              unCheckedChildren="禁止参与者添加选项"
            />
          </Form.Item>

          <Form.List
            name="options"
            rules={[
              {
                validator: async (_, options) => {
                  if (!options || options.length < 2) {
                    return Promise.reject(new Error('至少需要2个选项'))
                  }
                },
              },
            ]}
          >
            {(fields, { add, remove }, { errors }) => (
              <>
                {fields.map(({ key, name, ...restField }, index) => (
                  <Form.Item
                    label={index === 0 ? '选项列表' : ''}
                    required={false}
                    key={key}
                  >
                    <Form.Item
                      {...restField}
                      name={[name, 'text']}
                      validateTrigger={['onChange', 'onBlur']}
                      rules={[
                        {
                          required: true,
                          whitespace: true,
                          message: '请输入选项内容或删除此项',
                        },
                      ]}
                      noStyle
                    >
                      <Input
                        placeholder="选项内容"
                        style={{ width: voteType === 'SLIDER' ? '60%' : '90%' }}
                        size="large"
                      />
                    </Form.Item>
                    {voteType === 'SLIDER' ? (
                      <Form.Item {...restField} name={[name, 'maxScore']} noStyle>
                        <InputNumber
                          min={1}
                          max={100}
                          placeholder="满分"
                          style={{ width: '25%', marginLeft: '5%' }}
                          size="large"
                        />
                      </Form.Item>
                    ) : null}
                    {fields.length > 2 ? (
                      <MinusCircleOutlined
                        className="ml-4 cursor-pointer text-lg text-red-500 hover:text-red-700"
                        onClick={() => remove(name)}
                      />
                    ) : null}
                  </Form.Item>
                ))}
                <Form.Item>
                  <Button
                    type="dashed"
                    onClick={() => add()}
                    icon={<PlusOutlined />}
                    className="w-[90%]"
                    size="large"
                  >
                    添加新选项
                  </Button>
                  <Form.ErrorList errors={errors} />
                </Form.Item>
              </>
            )}
          </Form.List>

          <Form.Item className="mt-8 border-t border-gray-100 pt-4 text-center">
            <Space size="large">
              <Button onClick={() => navigate('/')} size="large">
                取消
              </Button>
              <Button
                type="primary"
                htmlType="submit"
                size="large"
                loading={loading}
                className="px-12"
                disabled={!isLoggedIn}
              >
                发布投票
              </Button>
            </Space>
          </Form.Item>
        </Form>
        </Card>
      ) : null}
    </div>
  )
}

export default Admin
