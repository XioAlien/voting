import React from 'react'
import { App as AntdApp, Spin } from 'antd'
import { useNavigate, useOutletContext } from 'react-router-dom'
import type { LayoutOutletContext } from './Layout'

interface AdminAccessRouteProps {
  children: React.ReactNode
}

const AdminAccessRoute: React.FC<AdminAccessRouteProps> = ({ children }) => {
  const navigate = useNavigate()
  const { message } = AntdApp.useApp()
  const { isAdmin, isAdminChecking, isLoggedIn } = useOutletContext<LayoutOutletContext>()
  const redirectedRef = React.useRef<'login' | 'home' | null>(null)

  React.useEffect(() => {
    if (isAdminChecking) {
      redirectedRef.current = null
      return
    }

    if (!isLoggedIn) {
      if (redirectedRef.current !== 'login') {
        redirectedRef.current = 'login'
        message.warning('请先登录')
        navigate('/login', { replace: true })
      }
      return
    }

    if (!isAdmin) {
      if (redirectedRef.current !== 'home') {
        redirectedRef.current = 'home'
        message.warning('无权限访问')
        navigate('/', { replace: true })
      }
      return
    }

    redirectedRef.current = null
  }, [isAdmin, isAdminChecking, isLoggedIn, message, navigate])

  if (isAdminChecking) {
    return (
      <div className="py-12 text-center">
        <Spin size="large" />
      </div>
    )
  }

  if (!isLoggedIn || !isAdmin) {
    return null
  }

  return <>{children}</>
}

export default AdminAccessRoute
