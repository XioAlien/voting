import React from 'react';
import { Layout as AntLayout, Menu } from 'antd';
import { Outlet, useNavigate } from 'react-router-dom';

const { Header, Content, Footer } = AntLayout;

const Layout: React.FC = () => {
  const navigate = useNavigate();

  return (
    <AntLayout className="min-h-screen">
      <Header className="flex items-center bg-white shadow-sm px-8">
        <div className="text-xl font-bold mr-8 cursor-pointer" onClick={() => navigate('/')}>
          VoteSystem
        </div>
        <Menu
          mode="horizontal"
          defaultSelectedKeys={['home']}
          items={[
            { key: 'home', label: '首页', onClick: () => navigate('/') },
            { key: 'admin', label: '管理后台', onClick: () => navigate('/admin') },
            { key: 'login', label: '登录', onClick: () => navigate('/login') }
          ]}
          className="flex-1 border-none"
        />
      </Header>
      <Content className="p-8 max-w-7xl mx-auto w-full mt-6 bg-white rounded-lg shadow-sm">
        <Outlet />
      </Content>
      <Footer className="text-center text-gray-500 mt-auto">
        VoteSystem ©2026 Created by Web Development Agent
      </Footer>
    </AntLayout>
  );
};

export default Layout;