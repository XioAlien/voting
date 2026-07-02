import React from 'react'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { App as AntdApp, ConfigProvider, theme } from 'antd'
import Layout from './components/Layout'
import AdminAccessRoute from './components/AdminAccessRoute'
import Home from './pages/Home'
import VoteDetail from './pages/VoteDetail'
import Login from './pages/Login'
import Register from './pages/Register'
import Admin from './pages/Admin'

function App() {
  return (
    <ConfigProvider
      theme={{
        algorithm: theme.defaultAlgorithm,
        token: {
          colorPrimary: '#1677ff',
        },
      }}
    >
      <AntdApp>
        <BrowserRouter>
          <Routes>
            <Route path="/" element={<Layout />}>
              <Route index element={<Home />} />
              <Route path="create" element={<Admin />} />
              <Route path="vote/:id" element={<VoteDetail />} />
              <Route
                path="admin"
                element={
                  <AdminAccessRoute>
                    <Admin showAdminModules />
                  </AdminAccessRoute>
                }
              />
            </Route>
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
          </Routes>
        </BrowserRouter>
      </AntdApp>
    </ConfigProvider>
  )
}

export default App
