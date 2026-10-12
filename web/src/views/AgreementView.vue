<template>
  <NavBar fallback="/register" />
  <!--
    ⚠️ 以下只是占位草稿，不是法律文本。上线前请按实际运营主体、备案信息和业务，
    请专业人士起草正式的《用户协议》和《隐私政策》替换这里的内容。
  -->
  <article class="page card doc">
    <template v-if="type === 'agreement'">
      <h1>用户协议</h1>
      <p class="draft">【草稿，正式上线前需替换为正式文本】</p>
      <h2>一、服务说明</h2>
      <p>摄影来为用户提供摄影师信息展示与拍摄预约撮合服务。平台不提供在线支付，拍摄费用、交通费、违约金等由摄影师与客户线下协商结算。</p>
      <h2>二、账号</h2>
      <p>用户使用手机号注册，应妥善保管密码。用户对其账号下的行为负责。</p>
      <h2>三、内容规范</h2>
      <p>用户上传的客片、简介、评价等内容须为本人拥有合法权利的内容，不得含有违法违规、侵权、色情低俗等信息。上传他人肖像须取得被拍摄者授权。平台有权删除违规内容并封禁账号。</p>
      <h2>四、预约与纠纷</h2>
      <p>预约在摄影师确认后成立，双方应按约定履行。因拍摄、付款产生的纠纷由双方协商解决。</p>
    </template>
    <template v-else>
      <h1>隐私政策</h1>
      <p class="draft">【草稿，正式上线前需替换为正式文本】</p>
      <h2>一、我们收集的信息</h2>
      <p>手机号（注册登录）、昵称、头像、简介、微信号（选填）、预约时填写的联系方式与拍摄信息；经你授权后的位置信息（仅用于计算与摄影师的距离）。</p>
      <h2>二、信息的使用与展示</h2>
      <p>你的手机号不会公开展示。摄影师确认你的预约后，你可以看到摄影师的微信号（未填写时为手机号）；你在预约中留下的联系方式仅对该摄影师可见。摄影师的接单位置坐标不会公开，只用于显示大致距离。</p>
      <h2>三、信息的存储与删除</h2>
      <p>信息存储于中国境内的服务器。如需注销账号或删除个人信息，请联系平台管理员。</p>
    </template>
  </article>
</template>

<script setup lang="ts">
import NavBar from '@/components/NavBar.vue'

defineProps<{ type: 'agreement' | 'privacy' }>()
</script>

<style lang="scss" scoped>
.doc { margin: 12px; line-height: 1.8; font-size: 14px; }
h1 { font-size: 22px; margin: 0 0 8px; }
h2 { font-size: 16px; margin: 18px 0 6px; }
p { margin: 0; color: #444; }
.draft { color: #e5483d; }
</style>
