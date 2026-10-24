# 图标来源与许可

本项目的图标不是手绘，也不是 Material Symbols，而是从 **[react-icons](https://github.com/react-icons/react-icons) 5.7.0** 的生成文件中抽取的 SVG 几何：

- `fi` → **Feather Icons**（MIT，Copyright 2013–2023 Cole Bemis）
- `lu` → **Lucide**（ISC，Copyright 2022 Lucide Contributors）
- 打包与再分发：react-icons（MIT，Copyright 2018–2024 react-icons contributors）

抽取结果固化在 `design/icons.json`，可用 `design/tools/extract-icons.mjs` 复现：

```bash
npm install react-icons@5.7.0
REACT_ICONS_DIR=./node_modules/react-icons node design/tools/extract-icons.mjs
```

预览板把这些定义内联成 `<symbol>` 雪碧图；Compose 实现从同一份 JSON 生成 `ImageVector`，因此两端图标几何完全一致。

注意：`react-icons` 是 React 组件库，不能在 Kotlin 中直接依赖；这里只使用其图标数据，不引入 React 运行时。MIT/ISC 均要求保留上述版权声明，本文件即为此用途。
