import React from 'react';
import { App as AntdApp, Card, Form, Input, Button, Typography } from 'antd';
import { useNavigate, useSearchParams } from 'react-router-dom';
import http from '../api/http';
import { getEnvelopeErrorMessage } from '../lib/errors';

const { Title } = Typography;

interface RegisterFormValues {
  username: string;
  email: string;
  password: string;
}

const Register: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { message } = AntdApp.useApp();
  const [loading, setLoading] = React.useState(false);
  const redirect = searchParams.get('redirect') || '/';

  const onFinish = async (values: RegisterFormValues) => {
    setLoading(true);
    try {
      const res = await http.post('/api/auth/register', {
        username: values.username,
        email: values.email,
        password: values.password,
      });
      if (res.data?.success && res.data?.data?.token) {
        localStorage.setItem('token', res.data.data.token);
        message.success('注册成功');
        setTimeout(() => {
          navigate(redirect);
        }, 500);
      } else {
        message.error(getEnvelopeErrorMessage(res.data?.message, '注册失败'));
      }
    } catch {
      message.error('注册失败，请检查输入');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <Card className="w-full max-w-md shadow-lg">
        <div className="text-center mb-8">
          <Title level={2}>加入 VoteSystem</Title>
          <p className="text-gray-500">创建新账号以发起或参与投票</p>
        </div>

        <Form
          name="register"
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
            label="邮箱"
            name="email"
            rules={[
              { required: true, message: '请输入邮箱！' },
              { type: 'email', message: '请输入有效的邮箱格式' }
            ]}
          >
            <Input size="large" placeholder="请输入邮箱" />
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
              注册
            </Button>
          </Form.Item>

          <div className="text-center mt-4">
            <span className="text-gray-500">已有账号？</span>
            <Button
              type="link"
              onClick={() => navigate(`/login?redirect=${encodeURIComponent(redirect)}`)}
              className="px-1"
            >
              返回登录
            </Button>
          </div>
        </Form>
      </Card>
    </div>
  );
};

export default Register;
