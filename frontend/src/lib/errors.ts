type ErrorResponseData = {
  message?: string
}

type ErrorWithResponse = {
  response?: {
    status?: number
    data?: ErrorResponseData
  }
}

const STATUS_MESSAGE_MAP: Record<number, string> = {
  400: '请求有误，请检查输入',
  401: '请先登录',
  403: '无权限访问',
  404: '内容不存在',
  409: '当前状态下无法完成该操作',
  500: '服务暂时不可用，请稍后重试',
}

function normalizeMessage(message?: string | null) {
  return message?.trim() || ''
}

function mapKnownBusinessMessage(message?: string | null) {
  const normalized = normalizeMessage(message)
  if (!normalized) {
    return null
  }

  if (normalized.includes('邀请码无效')) {
    return '邀请码无效'
  }
  if (normalized.includes('邀请码已过期')) {
    return '邀请码已过期'
  }
  if (normalized.includes('邀请码不能为空')) {
    return '请输入邀请码'
  }
  if (normalized.includes('成员数量已达上限')) {
    return '成员数量已达上限'
  }
  if (normalized.includes('投票不存在') || normalized.includes('未找到')) {
    return '内容不存在'
  }
  if (normalized.includes('未登录')) {
    return '请先登录'
  }
  if (normalized.includes('无权限')) {
    return '无权限访问'
  }
  if (normalized.includes('已投过票')) {
    return '你已提交过投票'
  }
  if (normalized.includes('用户名或密码')) {
    return '用户名或密码错误'
  }

  return null
}

export function logDevError(scope: string, error: unknown) {
  if (import.meta.env.DEV) {
    console.error(`[${scope}]`, error)
  }
}

export function getEnvelopeErrorMessage(rawMessage: unknown, fallback: string) {
  const mapped = mapKnownBusinessMessage(typeof rawMessage === 'string' ? rawMessage : '')
  return mapped || fallback
}

export function getUserFacingErrorMessage(error: unknown, fallback: string) {
  const maybeError = error as ErrorWithResponse
  const status = maybeError?.response?.status
  const rawMessage = maybeError?.response?.data?.message

  logDevError('api', error)

  const mappedMessage = mapKnownBusinessMessage(rawMessage)
  if (mappedMessage) {
    return mappedMessage
  }

  if (status && STATUS_MESSAGE_MAP[status]) {
    return STATUS_MESSAGE_MAP[status]
  }

  return fallback
}
