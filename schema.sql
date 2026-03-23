-- ============================================================
-- 巡察调查问卷系统 · 完整数据库设计
-- MySQL 8.0+
-- ============================================================

CREATE DATABASE IF NOT EXISTS survey_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE survey_db;

-- ------------------------------------------------------------
-- 1. 部门表（被巡察党组织列表，前端下拉）
-- ------------------------------------------------------------
CREATE TABLE departments (
  id          INT UNSIGNED  NOT NULL AUTO_INCREMENT,
  name        VARCHAR(64)   NOT NULL COMMENT '党组织 / 分厂名称',
  sort_order  SMALLINT      NOT NULL DEFAULT 0,
  created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uq_dept_name (name)
) COMMENT='部门（党组织）表';

-- ------------------------------------------------------------
-- 2. 员工表
-- ------------------------------------------------------------
CREATE TABLE employees (
  id            INT UNSIGNED  NOT NULL AUTO_INCREMENT,
  emp_no        VARCHAR(32)   NOT NULL COMMENT '工号，唯一',
  name          VARCHAR(64)   NOT NULL COMMENT '姓名',
  department_id INT UNSIGNED  NOT NULL COMMENT '所属部门',
  is_active     TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '1在职 0离职',
  created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uq_emp_no (emp_no),
  KEY idx_department (department_id),
  CONSTRAINT fk_emp_dept FOREIGN KEY (department_id) REFERENCES departments(id)
) COMMENT='员工表';

-- ------------------------------------------------------------
-- 3. 问卷模板表
--    两个分厂题目完全相同，共用同一套模板；survey_id 区分不同批次
-- ------------------------------------------------------------
CREATE TABLE surveys (
  id          INT UNSIGNED  NOT NULL AUTO_INCREMENT,
  title       VARCHAR(128)  NOT NULL COMMENT '问卷标题',
  description TEXT                   COMMENT '卷首说明语',
  year        SMALLINT      NOT NULL COMMENT '巡察年份',
  status      TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '0草稿 1发布 2关闭',
  starts_at   DATETIME               COMMENT '开放时间',
  ends_at     DATETIME               COMMENT '截止时间',
  created_by  VARCHAR(64)   NOT NULL COMMENT '创建人工号',
  created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  KEY idx_status_year (status, year)
) COMMENT='问卷表';

-- ------------------------------------------------------------
-- 4. 题目表
--    支持单选、文本、文件上传三种类型
--    section 区分题目所属大类，sort_order 控制展示顺序
-- ------------------------------------------------------------
CREATE TABLE questions (
  id          INT UNSIGNED  NOT NULL AUTO_INCREMENT,
  survey_id   INT UNSIGNED  NOT NULL COMMENT '所属问卷',
  section     VARCHAR(64)   NOT NULL COMMENT '题目分类，如"履职管理""一把手""问题线索"',
  sort_order  SMALLINT      NOT NULL DEFAULT 0 COMMENT '题目顺序',
  type        VARCHAR(16)   NOT NULL COMMENT 'radio=单选 text=文字 file=文件上传',
  content     TEXT          NOT NULL COMMENT '题目正文',
  is_required TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '是否必答',
  created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  KEY idx_survey_order (survey_id, sort_order),
  CONSTRAINT fk_q_survey FOREIGN KEY (survey_id) REFERENCES surveys(id)
) COMMENT='题目表';

-- ------------------------------------------------------------
-- 5. 选项表（仅 radio 类型题目使用）
-- ------------------------------------------------------------
CREATE TABLE question_options (
  id          INT UNSIGNED  NOT NULL AUTO_INCREMENT,
  question_id INT UNSIGNED  NOT NULL COMMENT '所属题目',
  sort_order  TINYINT       NOT NULL DEFAULT 0 COMMENT '选项顺序',
  content     VARCHAR(255)  NOT NULL COMMENT '选项文字',

  PRIMARY KEY (id),
  KEY idx_question (question_id),
  CONSTRAINT fk_opt_question FOREIGN KEY (question_id) REFERENCES questions(id)
) COMMENT='题目选项表';

-- ------------------------------------------------------------
-- 6. 答卷主表
--    每位员工每份问卷只能提交一次（工号 + 问卷联合唯一）
--    匿名填写时 emp_no 可传 anonymous，emp_name / dept_name 冗余存储
-- ------------------------------------------------------------
CREATE TABLE responses (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  survey_id    INT UNSIGNED    NOT NULL COMMENT '对应问卷',
  department_id INT UNSIGNED   NOT NULL COMMENT '被巡察党组织',
  emp_no       VARCHAR(32)     NOT NULL COMMENT '填写人工号（匿名时为 anonymous+uuid）',
  emp_name     VARCHAR(64)     NOT NULL COMMENT '姓名（冗余）',
  dept_name    VARCHAR(64)     NOT NULL COMMENT '部门名（冗余）',
  submitted_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ip           VARCHAR(45)              COMMENT '客户端 IP 备查',

  PRIMARY KEY (id),
  UNIQUE KEY uq_response (survey_id, emp_no),
  KEY idx_survey_dept (survey_id, department_id),
  CONSTRAINT fk_resp_survey FOREIGN KEY (survey_id) REFERENCES surveys(id),
  CONSTRAINT fk_resp_dept   FOREIGN KEY (department_id) REFERENCES departments(id)
) COMMENT='答卷主表';

-- ------------------------------------------------------------
-- 7. 答题明细表
--    每道题的答案单独一行，便于按题目统计分析
--    单选题：answer_text 存选项内容，option_id 存选项 ID
--    文字题：answer_text 存录入内容，option_id 为 NULL
--    文件题：answer_text 存文件存储路径，option_id 为 NULL
-- ------------------------------------------------------------
CREATE TABLE response_answers (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  response_id BIGINT UNSIGNED NOT NULL COMMENT '所属答卷',
  question_id INT UNSIGNED    NOT NULL COMMENT '对应题目',
  option_id   INT UNSIGNED             COMMENT '所选选项 ID（单选题）',
  answer_text TEXT                     COMMENT '答案内容（文字/文件路径/选项文字）',

  PRIMARY KEY (id),
  KEY idx_response   (response_id),
  KEY idx_question   (question_id),
  CONSTRAINT fk_ans_response FOREIGN KEY (response_id) REFERENCES responses(id),
  CONSTRAINT fk_ans_question FOREIGN KEY (question_id) REFERENCES questions(id)
) COMMENT='答题明细表';

-- ------------------------------------------------------------
-- 8. 文件上传表（问题线索反映的附件）
-- ------------------------------------------------------------
CREATE TABLE response_files (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  response_id  BIGINT UNSIGNED NOT NULL COMMENT '所属答卷',
  question_id  INT UNSIGNED    NOT NULL COMMENT '对应题目',
  original_name VARCHAR(255)   NOT NULL COMMENT '原始文件名',
  storage_path  VARCHAR(512)   NOT NULL COMMENT '服务器存储路径',
  mime_type     VARCHAR(64)             COMMENT '文件类型',
  file_size     INT UNSIGNED            COMMENT '文件大小（字节）',
  uploaded_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  KEY idx_response (response_id),
  CONSTRAINT fk_file_response FOREIGN KEY (response_id) REFERENCES responses(id)
) COMMENT='附件上传表';


-- ============================================================
-- 初始化数据
-- ============================================================

-- 部门
INSERT INTO departments (name, sort_order) VALUES
  ('高温部件分厂党支部', 10),
  ('成品分厂党支部',     20);

-- 问卷
INSERT INTO surveys (title, description, year, status, created_by) VALUES
  (
    '"巡察问题大家提"调查问卷',
    '您好！我们是2026年公司党委第一巡察组，根据公司党委统一部署，已经进驻您所在部门开展巡察。为深入、客观了解情况，请积极参与本次问卷调查。本问卷为匿名填写，结果仅用于巡察组工作，请放心填写。',
    2026,
    1,
    'admin'
  );

-- 题目（第一部分·一：履职管理，题号 1-10）
INSERT INTO questions (survey_id, section, sort_order, type, content) VALUES
(1, '部门和党组织履行核心职能责任、管理提升', 1,  'radio', '您所在党组织是否将学习习近平新时代中国特色社会主义思想、党的二十大和二十届历次全会精神、习近平总书记关于本行业本领域重要指示批示精神作为首要政治任务，并有效转化为推动工作的具体思路和举措？'),
(1, '部门和党组织履行核心职能责任、管理提升', 2,  'radio', '您认为本部门在贯彻执行上级和公司党委重要决议决定时，是否存在打折扣、搞变通、做选择，或"上下一般粗"、照搬照抄等问题？'),
(1, '部门和党组织履行核心职能责任、管理提升', 3,  'radio', '您认为本部门在推动公司深化改革、攻坚克难任务中，是否存在畏难情绪、等靠思想，或对深层次矛盾问题回避推诿、担当不足的情况？'),
(1, '部门和党组织履行核心职能责任、管理提升', 4,  'radio', '您认为本部门在防范化解重大风险（如合规经营、廉洁从业、廉洁风险、意识形态、安全生产风险等）方面，是否存在意识不到位、机制不健全、处置不力等问题？'),
(1, '部门和党组织履行核心职能责任、管理提升', 5,  'radio', '您认为本部门管理制度是否健全、适用？在实际执行中是否存在有制度不执行、执行打折扣或制度空转的情况？'),
(1, '部门和党组织履行核心职能责任、管理提升', 6,  'radio', '您认为所在党组织开展的"创先争优"主题实践活动是否与中心工作深度融合，是否充分发挥了支部战斗堡垒作用和党员先锋模范作用？'),
(1, '部门和党组织履行核心职能责任、管理提升', 7,  'radio', '您认为本部门在薪酬分配、绩效考核、岗位调配、评先评优等涉及职工切身利益的事项上，是否做到了程序规范、过程透明、结果公平公正？'),
(1, '部门和党组织履行核心职能责任、管理提升', 8,  'radio', '您认为本部门是否存在漠视职工利益、对职工诉求消极应付，在服务职工过程中推诿扯皮、不作为的情况？'),
(1, '部门和党组织履行核心职能责任、管理提升', 9,  'radio', '您认为本部门的人才队伍结构、能力素质是否满足部门高质量发展需求？'),
(1, '部门和党组织履行核心职能责任、管理提升', 10, 'radio', '您认为本部门或所在党组织落实巡视巡察、审计、专项治理等问题整改情况如何？'),
-- 第一部分·二：一把手，题号 11-16
(1, '"一把手"和班子成员履职行权', 11, 'radio', '您认为本部门"一把手"在落实全面从严治党"第一责任人"责任方面做得如何？是否存在抓班子、带队伍、表率作用发挥不够的问题？'),
(1, '"一把手"和班子成员履职行权', 12, 'radio', '您认为本部门"一把手"在重大事项决策过程中，是否违反民主集中制原则，存在搞"一言堂""小圈子"或听不进不同意见的情况？'),
(1, '"一把手"和班子成员履职行权', 13, 'radio', '您对本部门领导班子团结协作、协作配合推进工作方面的整体评价是？'),
(1, '"一把手"和班子成员履职行权', 14, 'radio', '您认为本部门领导班子成员是否认真履行党风廉政建设"一岗双责"？'),
(1, '"一把手"和班子成员履职行权', 15, 'radio', '您对本部门领导班子及成员廉洁自律的评价是？'),
(1, '"一把手"和班子成员履职行权', 16, 'radio', '您认为本部门领导班子（特别是一把手）在实际工作中，是否树立并践行了正确政绩观？'),
-- 开放题
(1, '"一把手"和班子成员履职行权', 17, 'text', '您认为本部门最突出的问题、风险点或不良倾向是什么？'),
(1, '"一把手"和班子成员履职行权', 18, 'text', '您对巡察工作的建议或需重点关注的领域？'),
-- 第二部分：问题线索反映
(1, '问题线索反映', 19, 'text', '问题描述（请具体、客观描述，可点人点事）'),
(1, '问题线索反映', 20, 'text', '相关说明'),
(1, '问题线索反映', 21, 'file', '上传相关文件（可选）');

-- 选项（每道单选题 4 个选项，题目 ID 1-16）
-- Q1
INSERT INTO question_options (question_id, sort_order, content) VALUES
(1,1,'认真组织研讨学习，并结合实际贯彻落实'),
(1,2,'组织传达学习，但转化运用不足，与实际工作结合不够紧密'),
(1,3,'学习形式化，未见明显成效'),
(1,4,'不了解');
-- Q2
INSERT INTO question_options (question_id, sort_order, content) VALUES
(2,1,'坚决贯彻执行，并结合实际细化落实，效果显著'),
(2,2,'基本能够执行，但主动性、创新性不足，效果一般'),
(2,3,'存在选择性执行或落实不到位的情况'),
(2,4,'不了解');
-- Q3
INSERT INTO question_options (question_id, sort_order, content) VALUES
(3,1,'勇于担当，主动破解难题'),
(3,2,'存在一定的畏难情绪，攻坚力度有待加强'),
(3,3,'回避矛盾，担当精神不足'),
(3,4,'不了解');
-- Q4
INSERT INTO question_options (question_id, sort_order, content) VALUES
(4,1,'高度重视风险防范，机制健全，防控有效'),
(4,2,'重视风险防范，但风险识别和应对能力有待提升'),
(4,3,'不重视风险防范，存在明显隐患'),
(4,4,'不了解');
-- Q5
INSERT INTO question_options (question_id, sort_order, content) VALUES
(5,1,'制度健全，执行严格有效'),
(5,2,'制度基本健全，但部分制度执行不到位'),
(5,3,'制度存在漏洞或明显不适应，且执行乏力'),
(5,4,'不了解');
-- Q6
INSERT INTO question_options (question_id, sort_order, content) VALUES
(6,1,'融合紧密，围绕中心，实效突出'),
(6,2,'有结合，但党支部和党员作用发挥不充分'),
(6,3,'形式大于内容，存在"两张皮"现象'),
(6,4,'不了解');
-- Q7
INSERT INTO question_options (question_id, sort_order, content) VALUES
(7,1,'制度健全、严格执行、公平公正'),
(7,2,'有制度，但执行中有时存在透明度不够或灵活性过大的情况'),
(7,3,'存在程序不规范或明显不公的现象'),
(7,4,'不了解');
-- Q8
INSERT INTO question_options (question_id, sort_order, content) VALUES
(8,1,'不存在，积极回应和解决职工关切'),
(8,2,'个别存在，效率或态度有待改进'),
(8,3,'较为突出，影响工作氛围和职工积极性'),
(8,4,'不了解');
-- Q9
INSERT INTO question_options (question_id, sort_order, content) VALUES
(9,1,'结构合理，素质较高，能够满足发展需要'),
(9,2,'基本满足，但高层次、专业化人才培养引进或人才队伍梯队建设有待加强'),
(9,3,'人才断层、结构失衡或激励不足问题较突出'),
(9,4,'不了解');
-- Q10
INSERT INTO question_options (question_id, sort_order, content) VALUES
(10,1,'党组织及领导重视、组织得力、整改到位'),
(10,2,'有牵头组织，但重视不够，整改推进迟缓'),
(10,3,'不重视整改工作，组织不到位，推进乏力'),
(10,4,'不了解');
-- Q11
INSERT INTO question_options (question_id, sort_order, content) VALUES
(11,1,'认真履责，严抓严管，表率作用好'),
(11,2,'有一定履责，但讲的多，履职力度和深度有待加强'),
(11,3,'履责不到位，部门政治生态有待改进'),
(11,4,'不了解');
-- Q12
INSERT INTO question_options (question_id, sort_order, content) VALUES
(12,1,'坚持民主集中制，科学民主决策'),
(12,2,'基本能做到民主决策，但有时酝酿沟通不充分'),
(12,3,'存在"一言堂"或变相个人主导决策现象'),
(12,4,'不了解');
-- Q13
INSERT INTO question_options (question_id, sort_order, content) VALUES
(13,1,'团结和谐，协作顺畅，战斗力强'),
(13,2,'总体团结，但有时沟通配合不够默契'),
(13,3,'存在不团结、不协调的现象'),
(13,4,'不了解');
-- Q14
INSERT INTO question_options (question_id, sort_order, content) VALUES
(14,1,'认真落实"一岗双责"，注重分管领域的廉洁风险防控及人员教育提醒'),
(14,2,'基本能够落实，但在常态化抓、抓具体方面有待深化'),
(14,3,'重业务轻党建，对分管领域党风廉政建设抓得不严'),
(14,4,'不了解');
-- Q15
INSERT INTO question_options (question_id, sort_order, content) VALUES
(15,1,'清正廉洁，作风过硬'),
(15,2,'整体较好，个别方面需注意'),
(15,3,'存在反映或值得关注的廉洁风险'),
(15,4,'不了解');
-- Q16
INSERT INTO question_options (question_id, sort_order, content) VALUES
(16,1,'树立正确的政绩观，决策和推进工作将群众满意度和长远效益放在首位'),
(16,2,'总体上能够树立正确导向，但个别项目或工作中可能存在重显绩轻潜绩、重速度轻质量的情况'),
(16,3,'工作中存在一定的功利化倾向，有时为了追求短期指标、表面成绩而忽视实际效果和群众感受'),
(16,4,'不了解');
