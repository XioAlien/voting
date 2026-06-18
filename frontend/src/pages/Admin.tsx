import React, { useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Col,
  DatePicker,
  Divider,
  Form,
  Input,
  InputNumber,
  Radio,
  Row,
  Space,
  Spin,
  Statistic,
  Switch,
  Typography,
  message,
} from 'antd'
import { MinusCircleOutlined, PlusOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import http from '../api/http'
import AdminRouteGuard from '../components/AdminRouteGuard'
import UserManagementPanel from '../components/UserManagementPanel'
import { getApiErrorMessage, hasStoredToken } from '../lib/auth'

const { Title, Paragraph } = Typography
const { RangePicker } = DatePicker
const { TextArea } = Input

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
  title: string
  description?: string
  type: 'CHOICE' | 'SLIDER'
  minChoices?: number
  maxChoices?: number
  forceAllOptions?: boolean
  allowCustomOptions?: boolean
  timeRange?: Array<{ toISOString: () => string }>
  options: VoteFormOption[]
}

const AdminOverview: React.FC = () => {
  const [dashboard, setDashboard] = React.useState<AdminDashboardData | null>(null)
  const [loading, setLoading] = React.useState(true)

  const fetchDashboard = React.useCallback(async () => {
    setLoading(true)
    try {
      const res = await http.get('/api/admin/dashboard')
      if (res.data?.success) {
        setDashboard(res.data.data)
      } else {
        message.error(res.data?.message || '获取管理员概览失败')
      }
    } catch (error) {
      message.error(getApiErrorMessage(error, '获取管理员概览失败'))
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
        message="管理员概览暂时不可用"
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

const Admin: React.FC = () => {
  const [form] = Form.useForm()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [voteType, setVoteType] = useState('CHOICE')
  const isLoggedIn = hasStoredToken()

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
        minChoices: values.minChoices || 1,
        maxChoices: values.maxChoices || 1,
        forceAllOptions: values.forceAllOptions || false,
        allowCustomOptions: values.allowCustomOptions || false,
        options: values.options.map((opt) => ({
          text: opt.text,
          maxScore: opt.maxScore || 100,
        })),
      }

      const res = await http.post('/api/votes', payload)
      if (res.data?.success) {
        message.success('投票创建成功')
        setTimeout(() => {
          navigate('/')
        }, 800)
      } else {
        message.error(res.data?.message || '创建失败')
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
          管理与创建中心
        </Title>
        <Paragraph className="!mb-0 text-gray-500">
          已登录用户可继续复用当前流程创建投票；管理员会额外看到全局统计与用户管理能力。
        </Paragraph>
      </div>

      <AdminRouteGuard>
        <AdminOverview />
        <UserManagementPanel />
      </AdminRouteGuard>

      {!isLoggedIn ? (
        <Card>
          <Alert
            type="warning"
            showIcon
            message="创建投票需要先登录"
            description="当前页面仍保留原有创建投票骨架，但提交接口要求携带现有 token。"
            action={
              <Button type="primary" onClick={() => navigate('/login')}>
                去登录
              </Button>
            }
          />
        </Card>
      ) : null}

      <Card className="shadow-sm" title="发起新投票">
        <Form
          form={form}
          layout="vertical"
          name="createVote"
          onFinish={onFinish}
          initialValues={{
            type: 'CHOICE',
            minChoices: 1,
            maxChoices: 1,
            forceAllOptions: false,
            allowCustomOptions: false,
            options: [
              { text: '选项1', maxScore: 100 },
              { text: '选项2', maxScore: 100 },
            ],
          }}
          onValuesChange={(changedValues) => {
            if (changedValues.type) {
              setVoteType(changedValues.type)
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
                {fields.map((field, index) => (
                  <Form.Item
                    label={index === 0 ? '选项列表' : ''}
                    required={false}
                    key={field.key}
                  >
                    <Form.Item
                      {...field}
                      name={[field.name, 'text']}
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
                      <Form.Item {...field} name={[field.name, 'maxScore']} noStyle>
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
                        onClick={() => remove(field.name)}
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
                确认发布
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}

export default Admin
