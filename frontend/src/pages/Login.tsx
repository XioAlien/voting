import React from 'react';
import { App as AntdApp, Card, Form, Input, Button, Typography } from 'antd';
import { useNavigate, useSearchParams } from 'react-router-dom';
import http from '../api/http';
import { getEnvelopeErrorMessage } from '../lib/errors';

const { Title } = Typography;

interface LoginFormValues {
  username: string;
  password: string;
}

const Login: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { message } = AntdApp.useApp();
  const [loading, setLoading] = React.useState(false);
  const redirect = searchParams.get('redirect') || '/';

  const onFinish = async (values: LoginFormValues) => {
    setLoading(true);
    try {
      const res = await http.post('/api/auth/login', {
        usernameOrEmail: values.username,
        password: values.password,
      });
      if (res.data?.success && res.data?.data?.token) {
        localStorage.setItem('token', res.data.data.token);
        message.success('登录成功');
        setTimeout(() => {
          navigate(redirect);
        }, 500);
      } else {
        message.error(getEnvelopeErrorMessage(res.data?.message, '登录失败'));
      }
    } catch {
      message.error('用户名或密码错误');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <Card className="w-full max-w-md shadow-lg">
        <div className="text-center mb-8">
          <Title level={2}>登录 VoteSystem</Title>
          <p className="text-gray-500">欢迎回来，请登录您的账号</p>
        </div>

        <Form
          name="login"
          layout="vertical"
          onFinish={onFinish}
          autoComplete="off"
        >
          <Form.Item
            label="用户名"
            name="username"
            rules={[{ required: true, message: '请输入用户名！' }]}
          >
            <Input size="large" placeholder="请输入用户名" />
          </Form.Item>

          <Form.Item
            label="密码"
            name="password"
            rules={[{ required: true, message: '请输入密码！' }]}
          >
            <Input.Password size="large" placeholder="请输入密码" />
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" className="w-full" size="large" loading={loading}>
              登录
            </Button>
          </Form.Item>

          <div className="text-center mt-4">
            <span className="text-gray-500">还没有账号？</span>
            <Button
              type="link"
              onClick={() => navigate(`/register?redirect=${encodeURIComponent(redirect)}`)}
              className="px-1"
            >
              立即注册
            </Button>
          </div>
        </Form>
      </Card>
    </div>
  );
};

export default Login;
