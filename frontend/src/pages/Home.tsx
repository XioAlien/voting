import React, { useCallback, useEffect, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Form,
  Input,
  List,
  Space,
  Tag,
  Typography,
  message,
  Spin,
} from 'antd'
import { useNavigate } from 'react-router-dom'
import http from '../api/http'
import { getApiErrorMessage, hasStoredToken, probeAdminAccess } from '../lib/auth'

const { Title, Paragraph } = Typography

interface VoteItem {
  id: number
  title: string
  description: string
  status: string
  type: string
  participants: number
}

const Home: React.FC = () => {
  const navigate = useNavigate()
  const [data, setData] = useState<VoteItem[]>([])
  const [loading, setLoading] = useState(true)
  const [joining, setJoining] = useState(false)
  const [isAdmin, setIsAdmin] = useState(false)
  const [joinForm] = Form.useForm()
  const isLoggedIn = hasStoredToken()

  const detectAdmin = useCallback(async () => {
    if (!isLoggedIn) {
      setIsAdmin(false)
      return
    }

    try {
      setIsAdmin(await probeAdminAccess())
    } catch {
      setIsAdmin(false)
    }
  }, [isLoggedIn])

  const fetchVotes = useCallback(async () => {
    try {
      const res = await http.get('/api/votes')
      if (res.data?.success) {
        setData(res.data.data || [])
      } else {
        message.error(`获取投票列表失败: ${res.data?.message || '未知错误'}`)
      }
    } catch (error) {
      message.error(getApiErrorMessage(error, '获取投票列表失败'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void fetchVotes()
    void detectAdmin()
  }, [detectAdmin, fetchVotes])

  const handleJoinByInvite = async (values: { voteId: string; inviteCode: string }) => {
    if (!isLoggedIn) {
      navigate('/login')
      return
    }

    setJoining(true)
    try {
      const voteId = values.voteId.trim()
      const res = await http.post(`/api/votes/${voteId}/join`, {
        inviteCode: values.inviteCode.trim(),
      })

      if (res.data?.success) {
        message.success('已加入投票')
        navigate(`/vote/${voteId}`)
      } else {
        message.error(res.data?.message || '加入投票失败')
      }
    } catch (error) {
      message.error(getApiErrorMessage(error, '加入投票失败'))
    } finally {
      setJoining(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <Title level={2} className="!mb-1">
            投票大厅
          </Title>
          <Paragraph className="!mb-0 text-gray-500">
            首页仅展示当前账号可访问的投票；邀请码投票加入后会自动进入你的可见范围。
          </Paragraph>
        </div>
        <Space wrap>
          {isAdmin ? <Tag color="volcano">当前账号具备管理员权限</Tag> : null}
          <Button type="primary" onClick={() => navigate('/admin')}>
            {isAdmin ? '进入管理与创建中心' : '发起投票'}
          </Button>
        </Space>
      </div>

      <Card title="通过邀请码加入投票">
        {!isLoggedIn ? (
          <Alert
            type="info"
            showIcon
            message="登录后可加入邀请码投票"
            description="邀请码投票不会出现在公开列表中，登录后填写投票 ID 与邀请码即可直接进入。"
            action={
              <Button type="primary" onClick={() => navigate('/login')}>
                去登录
              </Button>
            }
          />
        ) : (
          <Form layout="inline" form={joinForm} onFinish={handleJoinByInvite}>
            <Form.Item
              name="voteId"
              rules={[{ required: true, message: '请输入投票 ID' }]}
            >
              <Input placeholder="投票 ID" />
            </Form.Item>
            <Form.Item
              name="inviteCode"
              rules={[{ required: true, message: '请输入邀请码' }]}
            >
              <Input placeholder="邀请码" maxLength={32} />
            </Form.Item>
            <Form.Item>
              <Button type="primary" htmlType="submit" loading={joining}>
                加入投票
              </Button>
            </Form.Item>
          </Form>
        )}
      </Card>

      {loading ? (
        <div className="p-12 text-center">
          <Spin size="large" />
        </div>
      ) : (
        <List
          grid={{ gutter: 16, xs: 1, sm: 2, md: 3, lg: 3, xl: 4, xxl: 4 }}
          dataSource={data}
          locale={{ emptyText: '当前没有可访问的投票，可尝试通过邀请码加入。' }}
          renderItem={(item) => (
            <List.Item>
              <Card
                hoverable
                title={item.title}
                extra={
                  <div className="flex space-x-2">
                    <Tag color="blue">{item.type === 'SLIDER' ? '滑块评分' : '选项投票'}</Tag>
                    {item.status === 'active' ? (
                      <Tag color="green">进行中</Tag>
                    ) : (
                      <Tag color="default">已结束</Tag>
                    )}
                  </div>
                }
                actions={[
                  <Button type="link" onClick={() => navigate(`/vote/${item.id}`)}>
                    {item.status === 'active' ? '去投票' : '查看结果'}
                  </Button>,
                ]}
              >
                <p className="h-12 line-clamp-2 text-gray-500">{item.description}</p>
                <p className="mt-4 text-xs text-gray-400">参与票数: {item.participants}</p>
              </Card>
            </List.Item>
          )}
        />
      )}
    </div>
  )
}

export default Home
