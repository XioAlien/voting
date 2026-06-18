import React from 'react'
import {
  Alert,
  Button,
  Card,
  Input,
  message,
  Popconfirm,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd'
import type { TableProps } from 'antd'
import http from '../api/http'
import { getApiErrorMessage, issueAdminConfirmToken } from '../lib/auth'

const { Text } = Typography

interface AdminUser {
  id: number
  username: string
  email: string
  role: string
  builtinAdmin: boolean
  disabled: boolean
  deleted: boolean
  mustChangePassword: boolean
  createdAt?: string | null
  updatedAt?: string | null
}

interface UserPageData {
  items: AdminUser[]
  total: number
  page: number
  size: number
}

interface ApiEnvelope<T> {
  success?: boolean
  message?: string
  data?: T
}

function formatDate(value?: string | null) {
  if (!value) {
    return '-'
  }

  return new Date(value).toLocaleString('zh-CN')
}

const UserManagementPanel: React.FC = () => {
  const [users, setUsers] = React.useState<AdminUser[]>([])
  const [loading, setLoading] = React.useState(true)
  const [keywordInput, setKeywordInput] = React.useState('')
  const [keyword, setKeyword] = React.useState('')
  const [current, setCurrent] = React.useState(1)
  const [pageSize, setPageSize] = React.useState(10)
  const [total, setTotal] = React.useState(0)
  const [updatingUserId, setUpdatingUserId] = React.useState<number | null>(null)

  const fetchUsers = React.useCallback(
    async (page = current, size = pageSize, nextKeyword = keyword) => {
      setLoading(true)
      try {
        const res = await http.get<ApiEnvelope<UserPageData>>('/api/admin/users', {
          params: {
            page: page - 1,
            size,
            keyword: nextKeyword || undefined,
          },
        })

        if (res.data?.success && res.data.data) {
          setUsers(res.data.data.items || [])
          setTotal(res.data.data.total || 0)
          setCurrent((res.data.data.page || 0) + 1)
          setPageSize(res.data.data.size || size)
          return
        }

        message.error(res.data?.message || '获取用户列表失败')
      } catch (error) {
        message.error(getApiErrorMessage(error, '获取用户列表失败'))
      } finally {
        setLoading(false)
      }
    },
    [current, keyword, pageSize],
  )

  React.useEffect(() => {
    void fetchUsers(1, pageSize, keyword)
  }, [fetchUsers, keyword, pageSize])

  const handleSearch = () => {
    setCurrent(1)
    setKeyword(keywordInput.trim())
  }

  const handleRoleChange = async (user: AdminUser, nextRole: string) => {
    setUpdatingUserId(user.id)
    try {
      const confirmToken = await issueAdminConfirmToken('UPDATE_USER_ROLE', String(user.id))
      const res = await http.patch<ApiEnvelope<AdminUser>>(`/api/admin/users/${user.id}/role`, {
        role: nextRole,
        confirmToken,
      })

      if (res.data?.success) {
        message.success(`已将 ${user.username} 设置为 ${nextRole}`)
        await fetchUsers(current, pageSize, keyword)
        return
      }

      message.error(res.data?.message || '更新用户角色失败')
    } catch (error) {
      message.error(getApiErrorMessage(error, '更新用户角色失败'))
    } finally {
      setUpdatingUserId(null)
    }
  }

  const columns: TableProps<AdminUser>['columns'] = [
    {
      title: '用户',
      key: 'user',
      render: (_, record) => (
        <div>
          <div className="font-medium">{record.username}</div>
          <Text type="secondary">{record.email}</Text>
        </div>
      ),
    },
    {
      title: '角色',
      dataIndex: 'role',
      key: 'role',
      render: (value: string, record) => (
        <Space wrap>
          <Tag color={value === 'ADMIN' ? 'volcano' : 'blue'}>
            {value === 'ADMIN' ? '管理员' : '普通用户'}
          </Tag>
          {record.builtinAdmin ? <Tag color="gold">内置管理员</Tag> : null}
        </Space>
      ),
    },
    {
      title: '状态',
      key: 'status',
      render: (_, record) => (
        <Space wrap>
          <Tag color={record.disabled ? 'red' : 'green'}>
            {record.disabled ? '已禁用' : '正常'}
          </Tag>
          {record.mustChangePassword ? <Tag color="orange">需改密</Tag> : null}
        </Space>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (value?: string | null) => formatDate(value),
    },
    {
      title: '操作',
      key: 'actions',
      render: (_, record) => {
        const nextRole = record.role === 'ADMIN' ? 'USER' : 'ADMIN'
        const actionLabel = nextRole === 'ADMIN' ? '提升为管理员' : '降为普通用户'

        return (
          <Popconfirm
            title={`确认${actionLabel}？`}
            description="该操作会先申请一次确认令牌，再调用管理员接口完成角色更新。"
            onConfirm={() => handleRoleChange(record, nextRole)}
            okText="确认"
            cancelText="取消"
          >
            <Button
              size="small"
              loading={updatingUserId === record.id}
              type={nextRole === 'ADMIN' ? 'primary' : 'default'}
            >
              {actionLabel}
            </Button>
          </Popconfirm>
        )
      },
    },
  ]

  return (
    <Card
      title="用户管理"
      extra={
        <Space>
          <Input.Search
            allowClear
            placeholder="按用户名或邮箱搜索"
            value={keywordInput}
            onChange={(event) => setKeywordInput(event.target.value)}
            onSearch={handleSearch}
            className="w-64"
          />
          <Button onClick={() => void fetchUsers(current, pageSize, keyword)}>刷新</Button>
        </Space>
      }
    >
      <Alert
        type="info"
        showIcon
        className="mb-4"
        message="当前后端已稳定支持角色调整；禁用、删除用户仍受实体语义限制，前端只开放可用能力。"
      />

      <Table<AdminUser>
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={users}
        pagination={{
          current,
          pageSize,
          total,
          showSizeChanger: true,
        }}
        onChange={(pagination) => {
          void fetchUsers(pagination.current || 1, pagination.pageSize || pageSize, keyword)
        }}
      />
    </Card>
  )
}

export default UserManagementPanel
