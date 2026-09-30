<template>
  <view class="page">
    <view class="hero">
      <text class="label">可提现余额（元）</text>
      <text class="balance">{{ formatMoney(wallet.withdrawableBalance) }}</text>
      <view class="hero-row">
        <text class="sub">冻结 ¥{{ formatMoney(wallet.frozenBalance) }}</text>
        <text class="sub">送达即入账配送费</text>
      </view>
      <view class="btn-accent cash" @click="goWithdraw">申请提现</view>
    </view>

    <view class="card">
      <text class="section-title">收款账户</text>
      <text class="label">真实姓名</text>
      <input class="input" v-model="form.realName" placeholder="收款人姓名" />
      <text class="label">微信收款账号</text>
      <input class="input" v-model="form.wechatAccount" placeholder="微信号 / OpenID" />
      <text class="label">支付宝账号</text>
      <input class="input" v-model="form.alipayAccount" placeholder="支付宝账号" />
      <text class="label">银行卡号</text>
      <input class="input" v-model="form.bankCard" placeholder="银行卡号" />
      <view class="btn-primary" @click="save">保存收款信息</view>
    </view>

    <view class="menu card">
      <view class="menu-item" @click="goBilling">
        <text>资金流水</text>
        <text class="muted">配送入账 / 提现 ›</text>
      </view>
      <view class="menu-item" @click="goWithdrawList">
        <text>提现记录</text>
        <text class="muted">审核进度 ›</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { financeApi } from '../../api/http.js'
import { requireLogin } from '../../utils/auth.js'
import { formatMoney } from '../../utils/format.js'

const wallet = ref({
  withdrawableBalance: 0,
  frozenBalance: 0,
  withdrawFee: 1
})
const form = reactive({
  realName: '',
  wechatAccount: '',
  alipayAccount: '',
  bankCard: ''
})

async function load() {
  if (!requireLogin()) return
  wallet.value = await financeApi.wallet()
  form.realName = wallet.value.realName || ''
  form.wechatAccount = wallet.value.wechatAccount || ''
  form.alipayAccount = wallet.value.alipayAccount || ''
  form.bankCard = wallet.value.bankCard || ''
}

async function save() {
  wallet.value = await financeApi.updateSettlement({ ...form })
  uni.showToast({ title: '已保存', icon: 'success' })
}

function goWithdraw() {
  uni.navigateTo({ url: '/pages/finance/withdraw' })
}
function goBilling() {
  uni.navigateTo({ url: '/pages/finance/billing' })
}
function goWithdrawList() {
  uni.navigateTo({ url: '/pages/finance/withdraw?tab=list' })
}

onShow(load)
</script>

<style scoped>
.hero {
  padding: 40rpx 32rpx 48rpx;
  background:
    radial-gradient(ellipse 90% 80% at 100% 0%, rgba(232, 93, 4, 0.22), transparent 55%),
    linear-gradient(165deg, #0f3d2e 0%, #1b5e45 70%);
  color: #fff;
}
.label { display: block; font-size: 24rpx; color: rgba(255,255,255,0.75); }
.hero .label { margin-bottom: 8rpx; }
.balance {
  display: block;
  font-size: 64rpx;
  font-weight: 800;
  margin: 8rpx 0 16rpx;
}
.hero-row { display: flex; justify-content: space-between; margin-bottom: 28rpx; }
.sub { font-size: 24rpx; color: rgba(255,255,255,0.7); }
.cash { margin-top: 8rpx; }
.card .label { color: var(--muted); margin-bottom: 10rpx; margin-top: 8rpx; }
.section-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: var(--brand);
  margin-bottom: 12rpx;
}
.input {
  background: #f3f6f4;
  border-radius: 14rpx;
  padding: 22rpx 20rpx;
  margin-bottom: 12rpx;
}
.menu { padding: 0 28rpx; }
.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx 0;
  border-bottom: 1rpx solid var(--line);
}
.menu-item:last-child { border-bottom: none; }
</style>
