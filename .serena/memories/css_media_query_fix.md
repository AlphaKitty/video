# CSS媒体查询修复

## 问题描述
在 `frontend/src/assets/main.css` 文件中，`@media (min-width: 1024px)` 媒体查询设置了错误的样式：
- `body { display: flex; place-items: center; }`
- `#app { display: grid; grid-template-columns: 1fr 1fr; }`

这导致在大屏幕（≥1024px）上页面被强制分为两列布局，造成页面异常过窄。

## 解决方案
1. 移除了 `body` 的 flex 布局设置
2. 移除了 `#app` 的 grid 两列布局
3. 将 `#app` 基础样式从固定宽度改为全宽
4. 保持简洁的媒体查询，只设置必要的 padding

## 修复后的样式
```css
#app {
  width: 100%;
  min-height: 100vh;
  font-weight: normal;
}

@media (min-width: 1024px) {
  #app {
    padding: 0;
  }
}
```

这确保了应用在所有屏幕尺寸下都能正常显示，不会出现异常过窄的问题。