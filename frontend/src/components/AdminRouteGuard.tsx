import React from 'react'
import { Alert, Spin } from 'antd'
import { probeAdminAccess } from '../lib/auth'

interface AdminRouteGuardProps {
  children: React.ReactNode
  fallback?: React.ReactNode
  loadingFallback?: React.ReactNode
}

const AdminRouteGuard: React.FC<AdminRouteGuardProps> = ({
  children,
  fallback,
  loadingFallback,
}) => {
  const [loading, setLoading] = React.useState(true)
  const [isAdmin, setIsAdmin] = React.useState(false)

  React.useEffect(() => {
    let active = true

    const checkAdmin = async () => {
      try {
        const result = await probeAdminAccess()
        if (active) {
          setIsAdmin(result)
        }
      } catch {
        if (active) {
          setIsAdmin(false)
        }
      } finally {
        if (active) {
          setLoading(false)
        }
      }
    }

    void checkAdmin()

    return () => {
      active = false
    }
  }, [])

  if (loading) {
    return (
      <>
        {loadingFallback || (
          <div className="text-center py-12">
            <Spin size="large" />
          </div>
        )}
      </>
    )
  }

  if (!isAdmin) {
    return (
      <>
        {fallback || (
          <Alert
            type="info"
            showIcon
            message="当前账号没有管理员权限"
            description="可继续使用下方的创建投票能力；管理员专属的用户管理与全局概览仅对管理员开放。"
          />
        )}
      </>
    )
  }

  return <>{children}</>
}

export default AdminRouteGuard
