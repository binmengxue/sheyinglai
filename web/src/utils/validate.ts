import type { FormItemRule } from 'element-plus'

/** 和后端 AuthReqs.PASSWORD_REGEX 保持一致 */
export const PASSWORD_REGEX = /^(?=.*[A-Za-z])(?=.*\d).{8,32}$/

export const passwordRules: FormItemRule[] = [
  { required: true, message: '请输入密码', trigger: 'blur' },
  { pattern: PASSWORD_REGEX, message: '密码 8~32 位，需同时包含字母和数字', trigger: 'blur' },
]

export const PHONE_REGEX = /^1\d{10}$/
