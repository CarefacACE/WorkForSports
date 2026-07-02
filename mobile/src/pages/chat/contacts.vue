<template>
  <view class="page">
    <view class="empty">
      <text class="empty-icon">🔍</text>
      <text class="empty-text">添加联系人</text>
      <text class="empty-sub">搜索用户名或ID找到好友</text>
    </view>
  </view>
</template>
<style scoped>
.page { min-height: 100vh; background: #f5f6fa; }
.empty { display: flex; flex-direction: column; align-items: center; padding: 200rpx 0; }
.empty-icon { font-size: 80rpx; margin-bottom: 16rpx; }
.empty-text { font-size: 28rpx; color: #999; }
.empty-sub { font-size: 24rpx; color: #bbb; margin-top: 8rpx; }
</style>
