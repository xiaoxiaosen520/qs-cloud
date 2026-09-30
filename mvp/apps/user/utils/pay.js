import { payApi, orderApi } from '../api/http.js'

/** 当前运行端，用于后端选择支付参数形态 */
export function payClient() {
  // #ifdef MP-WEIXIN
  return 'MP_WEIXIN'
  // #endif
  // #ifdef APP-PLUS
  return 'APP'
  // #endif
  return 'H5'
}

function isSimulateMode(mode) {
  return mode === 'MOCK' || (mode && String(mode).includes('SIMULATE'))
}

/**
 * 待支付订单：自动选择或弹出支付方式
 */
export async function payOrderAuto(orderId) {
  const list = (await payApi.channels()) || []
  const enabled = list.filter((c) => c.enabled)
  if (!enabled.length) {
    throw new Error('无可用支付方式')
  }
  if (enabled.length === 1) {
    return payOrder(orderId, enabled[0].code)
  }
  return new Promise((resolve, reject) => {
    uni.showActionSheet({
      itemList: enabled.map((c) => c.name),
      success: async (res) => {
        try {
          const r = await payOrder(orderId, enabled[res.tapIndex].code)
          resolve(r)
        } catch (e) {
          reject(e)
        }
      },
      fail: reject
    })
  })
}

/**
 * 发起支付：prepay → 调起渠道 → confirm（模拟）或等待回调（真支付）
 */
export async function payOrder(orderId, channel) {
  const params = await payApi.prepay({
    orderId,
    channel,
    client: payClient()
  })

  if (isSimulateMode(params.mode)) {
    await confirmSimulate(params)
    await payApi.confirm(params.orderId)
    return params
  }

  if (params.mode === 'WECHAT_MP' && params.params) {
    await invokeWechatMp(params.params)
    await waitUntilPaid(params.orderId)
    return params
  }

  if (params.mode === 'WECHAT_APP' && params.params) {
    await invokeWechatApp(params.params)
    await waitUntilPaid(params.orderId)
    return params
  }

  if (params.mode === 'ALIPAY_APP' && params.params) {
    await invokeAlipayApp(params.params)
    await waitUntilPaid(params.orderId)
    return params
  }

  if (params.mode === 'ALIPAY_H5' && params.params && params.params.formHtml) {
    await invokeAlipayH5(params.params.formHtml)
    await waitUntilPaid(params.orderId)
    return params
  }

  throw new Error('暂不支持的支付方式: ' + (params.mode || channel))
}

function confirmSimulate(params) {
  return new Promise((resolve, reject) => {
    uni.showModal({
      title: params.channel === 'MOCK' ? '模拟支付' : '模拟支付（待接商户）',
      content: `${params.hint || '确认支付'}\n应付 ¥${params.amount}`,
      confirmText: '确认支付',
      success: (r) => (r.confirm ? resolve() : reject(new Error('cancel')))
    })
  })
}

function invokeWechatMp(p) {
  return new Promise((resolve, reject) => {
    uni.requestPayment({
      provider: 'wxpay',
      timeStamp: p.timeStamp,
      nonceStr: p.nonceStr,
      package: p.package,
      signType: p.signType || 'RSA',
      paySign: p.paySign,
      success: resolve,
      fail: reject
    })
  })
}

function invokeWechatApp(p) {
  return new Promise((resolve, reject) => {
    // uni-app App：orderInfo 用微信返回的调起字段
    const orderInfo = {
      appid: p.appid,
      partnerid: p.partnerid,
      prepayid: p.prepayid,
      package: p.package || 'Sign=WXPay',
      noncestr: p.noncestr,
      timestamp: p.timestamp,
      sign: p.sign
    }
    uni.requestPayment({
      provider: 'wxpay',
      orderInfo: p.orderInfo || orderInfo,
      success: resolve,
      fail: reject
    })
  })
}

function invokeAlipayApp(p) {
  return new Promise((resolve, reject) => {
    // 沙箱：安卓支付宝 SDK 默认正式环境，不切 SANDBOX 会报「商家订单参数异常」
    // #ifdef APP-PLUS
    try {
      if (String(p.sandbox) === 'true' && plus.os.name === 'Android') {
        const EnvUtils = plus.android.importClass('com.alipay.sdk.app.EnvUtils')
        EnvUtils.setEnv(EnvUtils.EnvEnum.SANDBOX)
      }
    } catch (e) {
      console.warn('alipay sandbox switch failed', e)
    }
    // #endif
    uni.requestPayment({
      provider: 'alipay',
      orderInfo: p.orderStr || p.orderInfo,
      success: resolve,
      fail: reject
    })
  })
}

/** H5：写入临时页并跳转支付宝收银台（需签约手机网站支付） */
function invokeAlipayH5(formHtml) {
  return new Promise((resolve, reject) => {
    try {
      // #ifdef H5
      const w = window.open('', '_blank')
      if (w) {
        w.document.write(formHtml)
        w.document.close()
        resolve()
        return
      }
      const div = document.createElement('div')
      div.style.display = 'none'
      div.innerHTML = formHtml
      document.body.appendChild(div)
      const form = div.querySelector('form')
      if (form) {
        form.submit()
        resolve()
      } else {
        reject(new Error('支付宝收银台表单无效'))
      }
      // #endif
      // #ifndef H5
      reject(new Error('当前端不支持支付宝 H5 跳转'))
      // #endif
    } catch (e) {
      reject(e)
    }
  })
}

/** 真支付以异步通知为准，短轮询订单状态 */
async function waitUntilPaid(orderId, tries = 12) {
  for (let i = 0; i < tries; i++) {
    try {
      const detail = await orderApi.detail(orderId)
      const status = detail && detail.order && detail.order.status
      if (status && status !== 'PENDING_PAY') {
        return detail
      }
    } catch (e) {
      /* ignore and retry */
    }
    await sleep(1500)
  }
  // 用户可能已付但通知延迟：不抛错，由业务页自行刷新
  return null
}

function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms))
}
