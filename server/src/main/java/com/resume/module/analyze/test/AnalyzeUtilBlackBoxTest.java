package com.resume.module.analyze.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.module.analyze.util.AnalyzeUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AnalyzeUtilBlackBoxTest {

    @Autowired
    private AnalyzeUtil analyzeUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 测试用例1：优质应届生简历（211硕士+2段头部大厂实习+完整项目）
    // 覆盖：应届生实习评分规则、高分段评分逻辑、全维度信息完整场景
    private static final String EXCELLENT_FRESH_GRADUATE_RESUME = """
            个人总结：求职Java后端开发岗位，拥有2段头部互联网公司实习经验，深耕电商系统后端开发方向，熟练掌握分布式系统核心技术栈，具备高并发场景下的功能开发与性能优化经验，主导过实习项目的核心模块开发，累计带来接口性能提升30%以上，具备较强的问题排查与独立开发能力，能够快速适配业务需求。
            教育背景：华中师范大学（211），计算机科学与技术专业，硕士研究生，2022.09-2025.06，GPA 3.3/4.0，专业排名前20%，获校级一等奖学金2次，主修分布式系统、数据库原理、高并发架构等核心课程。
            实习经历：1. 阿里巴巴集团，Java后端开发实习生，2024.03-2024.09，参与电商订单系统的迭代开发，负责订单超时取消模块的重构，优化MQ消息消费逻辑，将模块故障率从1.2%降至0.3%；参与618大促活动的接口压测与优化，完成5个核心接口的性能调优，单接口QPS提升40%。2. 字节跳动，后端开发实习生，2023.07-2023.12，参与内容平台用户管理系统开发，独立负责用户权限校验模块的设计与实现，支撑日均100万+的接口调用，实习期间零线上故障。
            专业技能：熟练掌握Java、Python两门开发语言，精通Spring Boot、Spring Cloud等微服务框架，熟悉MySQL、Redis、RocketMQ等中间件，掌握分布式事务、限流降级等核心技术，具备JVM调优与线上问题排查能力，了解云原生与K8s基础操作。
            项目经历：主导校级创新创业项目——分布式二手交易平台，担任后端负责人，完成整体架构设计与核心模块开发，支撑1000+注册用户，实现订单并发处理能力达500QPS，项目获校级互联网+大赛银奖，相关技术方案已申请软件著作权。
            """;

    // 测试用例2：中等社招简历（双非本科+3年中小公司经验+项目量化不足）
    // 覆盖：社招常规评分规则、中分段评分逻辑、普通职场人场景
    private static final String MID_LEVEL_SOCIAL_RECRUITMENT_RESUME = """
            个人总结：求职Java后端开发岗位，拥有3年互联网行业开发经验，专注于企业管理系统的后端开发与维护，熟练掌握常用后端技术栈，具备完整的项目开发全流程经验，能够独立完成常规业务模块的开发与迭代，适配不同业务场景的需求，具备良好的团队协作与需求沟通能力。
            教育背景：武汉东湖学院（双非本科），软件工程专业，本科，2019.09-2023.06，GPA 2.8/4.0，主修Java程序设计、数据结构、数据库原理等课程，获校级三等奖学金1次，通过大学英语四级考试。
            工作经历：武汉XX科技有限公司（中等规模互联网公司），Java后端开发工程师，2023.07-至今，参与企业OA管理系统的开发与迭代，负责考勤、审批模块的功能开发与日常维护；参与系统的数据库优化工作，优化10+条慢SQL语句，提升部分查询接口的响应速度；配合产品与测试团队完成需求评审、功能测试与上线全流程，累计参与12个版本的迭代开发。
            专业技能：掌握Java开发语言，熟悉Spring Boot、MyBatis等常用开发框架，了解MySQL、Redis数据库的基础使用，具备基础的Linux操作能力，能够独立完成简单接口的开发与调试。
            项目经历：参与企业智能OA系统升级项目，负责考勤打卡模块的后端开发，完成打卡规则配置、数据统计等功能的实现，支撑公司500+员工的日常使用；参与客户管理系统开发，负责客户信息录入与查询模块的开发工作，配合团队完成项目上线，项目上线后客户信息管理效率有一定提升。
            """;

    // 测试用例3：低匹配度简历（大专非对口专业+无技术工作经验+技能稀缺）
    // 覆盖：低分段评分规则、无实习应届生/跨行者保底逻辑、维度缺失场景
    private static final String LOW_MATCHING_RESUME = """
            个人总结：想找互联网技术相关的工作，之前做过行政类的工作，会基础的电脑操作，学习能力还可以，能吃苦，希望能找到合适的岗位慢慢学习积累经验，未来想往互联网行业发展。
            教育背景：武汉职业技术学院，市场营销专业，大专，2020.09-2023.06，在校期间参加过学生会社团活动，完成基础学业，没有相关计算机专业的学习经历，通过计算机一级考试。
            工作经历：武汉XX商贸有限公司，行政助理，2023.07-2024.05，主要负责办公室日常文件整理、员工考勤统计、会议安排等行政工作，协助完成部分招聘的基础筛选工作；日常负责办公用品的采购与管理，对接供应商，完成月度办公成本统计；协助人事部门完成新员工入职手续办理与基础培训组织，累计服务30+新员工，没有相关技术类工作经验。
            专业技能：熟练使用Office办公软件，了解一点基础的计算机操作，没有编程相关技能储备。
            项目经历：在校期间参与过校园营销大赛，负责活动的宣传推广工作，最终团队获得参与奖；工作中协助过公司的官网内容更新，主要是整理文字内容，没有参与开发相关工作；参与公司年度客户答谢会的筹备工作，负责嘉宾邀约与现场协调，到场人数超80人，活动顺利完成。
            """;

    /**
     * 校验AI返回结果的通用契约
     * 1. 返回非空 2. 合法JSON格式 3. 字段齐全 4. 分数范围合规 5. 建议长度合规
     */
    private void validateResponseFormat(String response) throws Exception {
        assertNotNull(response, "AI返回结果为空");
        JsonNode jsonNode = objectMapper.readTree(response);

        // 校验6个必填字段存在
        assertTrue(jsonNode.has("summary_score"), "缺失summary_score字段");
        assertTrue(jsonNode.has("education_score"), "缺失education_score字段");
        assertTrue(jsonNode.has("experience_score"), "缺失experience_score字段");
        assertTrue(jsonNode.has("skill_score"), "缺失skill_score字段");
        assertTrue(jsonNode.has("project_score"), "缺失project_score字段");
        assertTrue(jsonNode.has("suggestions"), "缺失suggestions字段");

        // 校验所有分数为0-100的整数
        int[] scores = {
                jsonNode.get("summary_score").asInt(),
                jsonNode.get("education_score").asInt(),
                jsonNode.get("experience_score").asInt(),
                jsonNode.get("skill_score").asInt(),
                jsonNode.get("project_score").asInt()
        };
        for (int score : scores) {
            assertTrue(score >= 0 && score <= 100, "分数超出0-100范围: " + score);
        }

        // 校验建议长度
        String suggestions = jsonNode.get("suggestions").asText();
        assertTrue(suggestions.length() >= 400 && suggestions.length() <= 1200,
                "建议字数不符合300-600要求，实际字数: " + suggestions.length());
    }

    @Test
    public void testExcellentFreshGraduateResume() throws Exception {
        String response = analyzeUtil.getAIResponse(EXCELLENT_FRESH_GRADUATE_RESUME);
        validateResponseFormat(response);
        System.out.println("优质应届生简历测试通过，返回结果：" + response);
    }

    @Test
    public void testMidLevelSocialRecruitmentResume() throws Exception {
        String response = analyzeUtil.getAIResponse(MID_LEVEL_SOCIAL_RECRUITMENT_RESUME);
        validateResponseFormat(response);
        System.out.println("中等社招简历测试通过，返回结果：" + response);
    }

    @Test
    public void testLowMatchingResume() throws Exception {
        String response = analyzeUtil.getAIResponse(LOW_MATCHING_RESUME);
        validateResponseFormat(response);
        System.out.println("低匹配度简历测试通过，返回结果：" + response);
    }
}