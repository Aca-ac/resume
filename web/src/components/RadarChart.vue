<!-- src/components/RadarChart.vue -->
<template>
  <div ref="chartRef" class="radar-chart" :style="{ height: chartHeight }"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick, onBeforeUnmount, computed } from 'vue'
import * as echarts from 'echarts'

export interface RadarDataItem {
  name: string
  value: number
  description?: string
}

const props = defineProps<{
  data: RadarDataItem[]
  height?: string
  maxScore?: number
}>()

const chartRef = ref<HTMLElement | null>(null)
let chartInstance: echarts.ECharts | null = null
const chartHeight = props.height || '280px'
const maxScore = props.maxScore || 100

// 默认维度配置 - 当数据为空时使用
const defaultDimensions: RadarDataItem[] = [
  { name: '专业技能', value: 0 },
  { name: '工作经验', value: 0 },
  { name: '学历背景', value: 0 },
  { name: '沟通能力', value: 0 },
  { name: '项目经验', value: 0 },
  { name: '综合素质', value: 0 },
]

// 计算实际使用的数据
const chartData = computed(() => {
  if (props.data && props.data.length > 0) {
    return props.data
  }
  return defaultDimensions
})

function initChart() {
  if (!chartRef.value) return

  chartInstance = echarts.init(chartRef.value)
  window.addEventListener('resize', handleResize)
  updateChart()
}

function updateChart() {
  if (!chartInstance) return

  const dimensions = chartData.value.map(d => d.name)
  const values = chartData.value.map(d => d.value)

  const option = {
    radar: {
      indicator: dimensions.map(name => ({
        name,
        max: maxScore,
        // 显示文字标签
      })),
      shape: 'circle',
      center: ['50%', '50%'],
      radius: '65%',
      axisName: {
        color: '#3d6b57',
        fontSize: 12,
        fontWeight: 500,
        padding: [2, 0],
        // 当维度名称过长时换行
        formatter: (name: string) => {
          if (name.length > 6) {
            return name.slice(0, 6) + '\n' + name.slice(6)
          }
          return name
        }
      },
      splitArea: {
        areaStyle: {
          color: ['rgba(100, 163, 134, 0.02)', 'rgba(100, 163, 134, 0.05)']
        }
      },
      axisLine: {
        lineStyle: {
          color: 'rgba(100, 163, 134, 0.2)'
        }
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(100, 163, 134, 0.15)'
        }
      }
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: values,
            name: '匹配度',
            areaStyle: {
              color: 'rgba(100, 163, 134, 0.3)'
            },
            lineStyle: {
              color: '#64A386',
              width: 2
            },
            itemStyle: {
              color: '#64A386'
            }
          }
        ],
        symbol: 'circle',
        symbolSize: 6,
        label: {
          show: true,
          formatter: (params: any) => {
            // 如果 maxScore 是 100，显示百分比
            if (maxScore === 100) {
              return params.value + '%'
            }
            return params.value + '分'
          },
          fontSize: 11,
          color: '#4f6b5d'
        }
      }
    ],
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        const data = params.data
        if (!data) return ''
        let html = `<div style="font-weight:600;margin-bottom:6px;">📊 各维度匹配度</div>`
        const dims = dimensions
        const vals = data.value
        dims.forEach((name, idx) => {
          const score = vals[idx] || 0
          const percent = maxScore === 100 ? score : Math.round((score / maxScore) * 100)
          html += `<div style="display:flex;justify-content:space-between;padding:3px 0;border-bottom:1px solid #f0f0f0;">
            <span>${name}</span>
            <span style="font-weight:600;color:#64A386;">${percent}%</span>
          </div>`
        })
        return html
      }
    },
    grid: {
      containLabel: true
    }
  }

  chartInstance.setOption(option, true)
}

function handleResize() {
  chartInstance?.resize()
}

// 监听数据变化
watch(() => props.data, async () => {
  await nextTick()
  updateChart()
}, { deep: true })

// 监听高度变化
watch(() => props.height, async () => {
  await nextTick()
  chartInstance?.resize()
})

onMounted(() => {
  initChart()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
  chartInstance = null
})
</script>

<style scoped>
.radar-chart {
  width: 100%;
  min-height: 200px;
}
</style>