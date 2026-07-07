<template>
  <router-view v-slot="{ Component, route }">
    <Suspense>
      <template #default>
        <transition name="page">
          <component :is="Component" :key="route.path" />
        </transition>
      </template>
      <template #fallback>
        <div class="route-loading">Loading...</div>
      </template>
    </Suspense>
  </router-view>
</template>