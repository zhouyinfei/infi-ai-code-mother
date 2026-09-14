<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  content: string
}>()

interface ParsedBlock {
  type: 'text' | 'tool-call' | 'code-block'
  text?: string
  toolName?: string
  filePath?: string
  lang?: string
  code?: string
}

/**
 * 解析消息内容，提取 [工具调用] 标题和 ``` 代码块
 */
const blocks = computed(() => {
  const result: ParsedBlock[] = []
  const text = props.content || ''

  // 按行处理
  const lines = text.split('\n')
  let i = 0

  while (i < lines.length) {
    const line = lines[i]

    // 匹配 [工具调用] 写入文件 xxx
    const toolMatch = line.match(/^\[工具调用\]\s*(\S+)\s*(.+)$/)
    if (toolMatch) {
      result.push({
        type: 'tool-call',
        toolName: toolMatch[1],
        filePath: toolMatch[2].trim(),
      })
      i++
      continue
    }

    // 匹配 ```lang 代码块开始
    const codeStartMatch = line.match(/^```(\w*)$/)
    if (codeStartMatch) {
      const lang = codeStartMatch[1] || ''
      const codeLines: string[] = []
      i++
      while (i < lines.length && !lines[i].startsWith('```')) {
        codeLines.push(lines[i])
        i++
      }
      result.push({
        type: 'code-block',
        lang,
        code: codeLines.join('\n'),
      })
      // 跳过结束 ```
      if (i < lines.length) i++
      continue
    }

    // 普通文本
    result.push({ type: 'text', text: line })
    i++
  }

  return result
})
</script>

<template>
  <div class="message-content">
    <template v-for="(block, idx) in blocks" :key="idx">
      <!-- 工具调用标题 -->
      <div v-if="block.type === 'tool-call'" class="tool-call-header">
        <span class="tool-call-label">[工具调用]</span>
        <span class="tool-call-name">{{ block.toolName }}</span>
        <span class="tool-call-path">{{ block.filePath }}</span>
      </div>

      <!-- 代码块 -->
      <div v-else-if="block.type === 'code-block'" class="code-block-wrapper">
        <div class="code-block-lang" v-if="block.lang">{{ block.lang }}</div>
        <pre class="code-block"><code>{{ block.code }}</code></pre>
      </div>

      <!-- 普通文本 -->
      <span v-else-if="block.text">{{ block.text }}<br /></span>
    </template>
  </div>
</template>

<style scoped>
.message-content {
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}

/* 工具调用标题 */
.tool-call-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 8px 0 4px;
  font-size: 13px;
  font-weight: 500;
}

.tool-call-label {
  color: #8c8c8c;
}

.tool-call-name {
  color: #1677ff;
  font-weight: 600;
}

.tool-call-path {
  color: #595959;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
}

/* 代码块 */
.code-block-wrapper {
  margin: 4px 0 8px;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  overflow: hidden;
  background: #fafafa;
}

.code-block-lang {
  padding: 4px 12px;
  font-size: 12px;
  color: #8c8c8c;
  background: #f0f0f0;
  border-bottom: 1px solid #e8e8e8;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
}

.code-block {
  margin: 0;
  padding: 12px;
  font-size: 13px;
  line-height: 1.6;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  overflow-x: auto;
  white-space: pre;
  color: #262626;
  background: transparent;
}
</style>
