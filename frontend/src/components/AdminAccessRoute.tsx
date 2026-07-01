import React from 'react'
import { App as AntdApp, Spin } from 'antd'
import { useNavigate } from 'react-router-dom'
import { hasStoredToken, probeAdminAccess } from '../lib/auth'
import { logDevError } from '../lib/errors'

interface AdminAccessRouteProps {
  children: React.ReactNode
}

const AdminAccessRoute: React.FC<AdminAccessRouteProps> = ({ children }) => {
  const navigate = useNavigate()
  const { message } = AntdApp.useApp()
  const [checking, setChecking] = React.useState(true)
  const [allowed, setAllowed] = React.useState(false)

  React.useEffect(() => {
    let active = true

    const verifyAccess = async () => {
      if (!hasStoredToken()) {
        message.warning('请先登录')
        navigate('/login', { replace: true })
        if (active) {
          setChecking(false)
        }
        return
      }

      try {
        const isAdmin = await probeAdminAccess()
        if (!active) {
          return
        }

        if (!isAdmin) {
          message.warning('无权限访问')
          navigate('/', { replace: true })
          setAllowed(false)
          setChecking(false)
          return
        }

        setAllowed(true)
      } catch (error) {
        logDevError('admin-route', error)
        if (!active) {
          return
        }
        message.error('页面暂时不可用')
        navigate('/', { replace: true })
      } finally {
        if (active) {
          setChecking(false)
        }
      }
    }

    void verifyAccess()

    return () => {
      active = false
    }
  }, [navigate])

  if (checking) {
    return (
      <div className="py-12 text-center">
        <Spin size="large" />
      </div>
    )
  }

  if (!allowed) {
    return null
  }

  return <>{children}</>
}

export default AdminAccessRoute
