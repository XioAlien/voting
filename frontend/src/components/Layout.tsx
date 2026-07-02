import React from 'react'
import { App as AntdApp, Button, Layout as AntLayout, Menu } from 'antd'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { clearStoredToken, hasStoredToken, probeAdminAccess } from '../lib/auth'
import { logDevError } from '../lib/errors'

const { Header, Content, Footer } = AntLayout

const Layout: React.FC = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const { message } = AntdApp.useApp()
  const [isAdmin, setIsAdmin] = React.useState(false)
  const [authVersion, setAuthVersion] = React.useState(0)
  const isLoggedIn = hasStoredToken()

  React.useEffect(() => {
    let active = true

    const detectAdmin = async () => {
      if (!hasStoredToken()) {
        setIsAdmin(false)
        return
      }

      try {
        const result = await probeAdminAccess()
        if (active) {
          setIsAdmin(result)
        }
      } catch (error) {
        logDevError('layout-admin-check', error)
        if (active) {
          setIsAdmin(false)
        }
      }
    }

    void detectAdmin()

    return () => {
      active = false
    }
  }, [location.pathname, authVersion])

  const handleLogout = () => {
    clearStoredToken()
    setIsAdmin(false)
    setAuthVersion((value) => value + 1)
    message.success('已退出登录')
    navigate('/')
  }

  const items = [
    { key: 'home', label: '首页', onClick: () => navigate('/') },
    { key: 'create', label: '创建投票', onClick: () => navigate('/create') },
    ...(isAdmin ? [{ key: 'admin', label: '管理后台', onClick: () => navigate('/admin') }] : []),
    isLoggedIn
      ? {
          key: 'logout',
          label: (
            <Button type="link" className="!px-0" onClick={handleLogout}>
              退出登录
            </Button>
          ),
        }
      : { key: 'login', label: '登录', onClick: () => navigate('/login') },
  ]

  const selectedKey = location.pathname.startsWith('/admin')
    ? 'admin'
    : location.pathname.startsWith('/create')
      ? 'create'
      : location.pathname.startsWith('/login')
        ? 'login'
        : !isLoggedIn
          ? 'home'
        : 'home'

  return (
    <AntLayout className="min-h-screen">
      <Header className="flex items-center bg-white shadow-sm px-8">
        <div className="text-xl font-bold mr-8 cursor-pointer" onClick={() => navigate('/')}>
          VoteSystem
        </div>
        <Menu
          mode="horizontal"
          selectedKeys={[selectedKey]}
          items={items}
          className="flex-1 border-none"
        />
      </Header>
      <Content className="p-8 max-w-7xl mx-auto w-full mt-6 bg-white rounded-lg shadow-sm">
        <Outlet />
      </Content>
      <Footer className="text-center text-gray-500 mt-auto">
        VoteSystem ©2026
      </Footer>
    </AntLayout>
  )
}

export default Layout
