import React from 'react'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { ConfigProvider, theme } from 'antd'
import Layout from './components/Layout'
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
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Layout />}>
            <Route index element={<Home />} />
            <Route path="vote/:id" element={<VoteDetail />} />
            <Route path="admin" element={<Admin />} />
          </Route>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
        </Routes>
      </BrowserRouter>
    </ConfigProvider>
  )
}

export default App