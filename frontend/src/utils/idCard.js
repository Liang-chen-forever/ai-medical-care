const WEIGHTS = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2]
const CHECK_CODES = ['1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2']

export function getIdCardValidationMessage(value) {
  const idCard = String(value || '').trim().toUpperCase()
  if (!idCard) return '身份证号不能为空'
  if (idCard.length !== 18) return '身份证号必须为18位'

  const body = idCard.slice(0, 17)
  if (!/^\d{17}$/.test(body) || !/^[0-9X]$/.test(idCard.charAt(17))) {
    return '身份证号格式不正确'
  }

  const checksum = body.split('').reduce(
    (sum, digit, index) => sum + Number(digit) * WEIGHTS[index],
    0
  )
  if (CHECK_CODES[checksum % 11] !== idCard.charAt(17)) {
    return '身份证号校验码不正确，请检查'
  }

  const year = Number(body.slice(6, 10))
  const month = Number(body.slice(10, 12))
  const day = Number(body.slice(12, 14))
  const date = new Date(year, month - 1, day)
  const validDate = year >= 1900 && year <= 2100
    && date.getFullYear() === year
    && date.getMonth() === month - 1
    && date.getDate() === day

  return validDate ? '' : '身份证号中的出生日期不合法'
}
