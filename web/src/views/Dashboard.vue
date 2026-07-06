<template>
  <div>
    <h2>Dashboard</h2>
    <el-row :gutter="16">
      <el-col :span="8"><el-card><h3>Resumes</h3><p>Manage and optimize your CV.</p><el-button @click="$router.push('/resumes')">Open</el-button></el-card></el-col>
      <el-col :span="8"><el-card><h3>Job Match</h3><p>Compare resume with JD.</p><el-button @click="$router.push('/match')">Open</el-button></el-card></el-col>
      <el-col :span="8"><el-card><h3>Interview</h3><p>Practice with AI interviewer.</p><el-button @click="$router.push('/interview/start')">Open</el-button></el-card></el-col>
    </el-row>
  </div>
</template>
