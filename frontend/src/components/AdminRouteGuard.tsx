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
            title="无权限访问"
          />
        )}
      </>
    )
  }

  return <>{children}</>
}

export default AdminRouteGuard
