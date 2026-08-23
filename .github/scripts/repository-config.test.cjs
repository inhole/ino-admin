const { readFileSync } = require('node:fs');
const { test } = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');

const repositoryRoot = path.resolve(__dirname, '..', '..');
const issueTemplateDirectory = path.join(repositoryRoot, '.github', 'ISSUE_TEMPLATE');

function readIssueForm(fileName) {
  return readFileSync(path.join(issueTemplateDirectory, fileName), 'utf8');
}

function readWorkflow(fileName) {
  return readFileSync(path.join(repositoryRoot, '.github', 'workflows', fileName), 'utf8');
}

function readRepositoryFile(...segments) {
  return readFileSync(path.join(repositoryRoot, ...segments), 'utf8');
}

function assertIncludesAll(content, values) {
  for (const value of values) {
    assert.ok(content.includes(value), `expected form to include ${value}`);
  }
}

function assertRequiredFields(content, fieldIds) {
  for (const fieldId of fieldIds) {
    const requiredField = new RegExp(
      `id: ${fieldId}[\\s\\S]*?validations:\\s*\\n\\s*required: true`,
    );
    assert.match(content, requiredField, `expected ${fieldId} to be required`);
  }
}

test('기능 이슈 폼은 공통 완료 조건과 사용자 가치를 수집한다', () => {
  const content = readIssueForm('feature.yml');

  assertIncludesAll(content, [
    'name:',
    'description:',
    'body:',
    'id: user-value',
    'id: scope',
    'id: acceptance-criteria',
    'id: test-plan',
  ]);
  assertRequiredFields(content, ['user-value', 'scope', 'acceptance-criteria', 'test-plan']);
});

test('버그 이슈 폼은 재현 절차와 공통 완료 조건을 수집한다', () => {
  const content = readIssueForm('bug.yml');

  assertIncludesAll(content, [
    'name:',
    'description:',
    'body:',
    'id: reproduction',
    'id: expected',
    'id: actual',
    'id: impact',
    'id: acceptance-criteria',
    'id: test-plan',
  ]);
  assertRequiredFields(content, [
    'reproduction',
    'expected',
    'actual',
    'impact',
    'acceptance-criteria',
    'test-plan',
  ]);
});

test('기술 작업 이슈 폼은 산출물과 공통 완료 조건을 수집한다', () => {
  const content = readIssueForm('technical-task.yml');

  assertIncludesAll(content, [
    'name:',
    'description:',
    'body:',
    'id: deliverable',
    'id: scope',
    'id: acceptance-criteria',
    'id: test-plan',
  ]);
  assertRequiredFields(content, ['deliverable', 'scope', 'acceptance-criteria', 'test-plan']);
});

test('dev 빠른 CI는 dev push와 PR에서 단위 검증만 실행한다', () => {
  const content = readWorkflow('dev-ci.yml');

  assertIncludesAll(content, [
    'name: Dev CI',
    'push:',
    'pull_request:',
    'branches: [dev]',
    'dev-ci-${{ github.workflow }}-${{ github.event.pull_request.number || github.ref }}',
    'cancel-in-progress: true',
    'backend-unit:',
    './gradlew test architectureTest',
    'frontend-unit:',
    'npm run lint',
    'npm run typecheck',
    'npm test',
  ]);
});

test('infra 전용 빠른 CI는 dev의 infra 변경만 Compose 설정을 검증한다', () => {
  const content = readWorkflow('dev-infra.yml');

  assertIncludesAll(content, [
    'name: Dev Infra CI',
    'push:',
    'pull_request:',
    'branches: [dev]',
    'paths:',
    '- infra/**',
    'dev-infra-${{ github.workflow }}-${{ github.event.pull_request.number || github.ref }}',
    'cancel-in-progress: true',
    'docker compose -f infra/compose.yaml config',
  ]);
});

test('main 통합 CI는 main 대상 PR과 수동 실행에서 전체 검증을 유지한다', () => {
  const content = readWorkflow('ci.yml');

  assertIncludesAll(content, [
    'name: Main Integration CI',
    'pull_request:',
    'branches: [main]',
    'workflow_dispatch:',
    './gradlew clean test integrationTest architectureTest',
    'npm run build',
    'npm run test:e2e',
  ]);
  assert.ok(!content.includes('push:'), 'main 통합 CI는 push에서 실행하면 안 됩니다');
});

test('개발 규칙 문서는 dev 배치 흐름과 이슈 연결 규칙을 일관되게 설명한다', () => {
  assertIncludesAll(readRepositoryFile('AGENTS.md'), [
    'dev',
    'GitHub Actions',
    'Refs: #',
    'Closes #',
  ]);
  assertIncludesAll(readRepositoryFile('docs', 'development', 'branch-strategy.md'), [
    'feature branch → dev',
    'dev → main',
    'merge commit',
    'fast-forward',
  ]);
  assertIncludesAll(readRepositoryFile('docs', 'development', 'commit-convention.md'), [
    'type: 한글 변경사항',
    'Refs: #123',
  ]);
  assertIncludesAll(readRepositoryFile('.docs', 'PROJECT_PLAN.md'), [
    '장기 dev',
    'test-first',
    'CI 검증 완료',
  ]);
});

test('루트 지침은 일반 이슈 작업과 dev 배치 전달 스킬의 책임을 분리한다', () => {
  const agents = readRepositoryFile('AGENTS.md');

  assertIncludesAll(agents, [
    '`ino-admin-work-on-issue`',
    'feature/Codex → `dev` PR',
    '`ino-admin-deliver-change`',
    'for `dev` → `main` batch delivery only',
  ]);
});

test('배치 전달 스킬은 동기화된 원격 SHA와 원격 커밋 범위를 감사한다', () => {
  const skill = readRepositoryFile(
    '.agents',
    'skills',
    'ino-admin-deliver-change',
    'SKILL.md',
  );

  assertIncludesAll(skill, [
    'git fetch origin main dev',
    'git rev-parse dev',
    'git rev-parse origin/dev',
    'origin/main..origin/dev',
    '정확한 `origin/dev` SHA',
  ]);
});

test('최초 부트스트랩의 사전 정책 커밋 예외는 고정된 SHA와 이슈에만 한정된다', () => {
  const skill = readRepositoryFile(
    '.agents',
    'skills',
    'ino-admin-deliver-change',
    'SKILL.md',
  );
  const verification = readRepositoryFile(
    'docs',
    'development',
    'dev-batch-workflow-verification.md',
  );

  for (const content of [skill, verification]) {
    assertIncludesAll(content, [
      '최초 부트스트랩 커밋 예외',
      'Issue #40',
      '84f5623',
      '0323e9d',
      'c11dfad',
      '1f30a85',
      '4167f81',
      'PR 본문',
      '이후 커밋에는 적용하지 않는다',
    ]);
  }
});

test('부트스트랩 문서는 실제 Actions 체크와 수동 정책 검토를 구분한다', () => {
  const branchStrategy = readRepositoryFile('docs', 'development', 'branch-strategy.md');
  const implementationPlan = readRepositoryFile(
    'docs',
    'superpowers',
    'plans',
    '2026-08-23-dev-batch-pr-workflow.md',
  );

  for (const content of [branchStrategy, implementationPlan]) {
    assertIncludesAll(content, [
      '최초 1회 부트스트랩',
      '새 `PR Policy`는 실행되지 않는다',
      '자동 `PR Policy` check가 없다',
      '`Main Integration CI`',
      '기존 PR 정책을 사람이 직접 대조',
      'required check',
      '같은 저장소',
    ]);
    assert.ok(
      !content.includes('기존 `PR Policy`와 `CI`'),
      '실행되지 않는 기존 PR Policy를 자동 부트스트랩 검사로 안내하면 안 됩니다',
    );
  }
});

test('Phase 완료 검증은 로컬 전체 실행을 강제하지 않고 Actions와 동일 Milestone을 기준으로 한다', () => {
  const agents = readRepositoryFile('AGENTS.md');
  const projectPlan = readRepositoryFile('.docs', 'PROJECT_PLAN.md');

  assertIncludesAll(agents, [
    'GitHub Issue',
    'same Milestone',
  ]);
  assert.ok(
    !projectPlan.includes('각 Phase 완료 시 최소한 다음을 실행한다.'),
    'Phase 완료에 로컬 전체 실행을 강제하면 안 됩니다',
  );
  assert.ok(
    !projectPlan.includes('backend: clean test + integrationTest + architectureTest'),
    'Phase 완료에 backend 전체 테스트를 강제하면 안 됩니다',
  );
  assertIncludesAll(projectPlan, [
    'GitHub Actions',
    '로컬 명령은 디버깅 또는 단일 테스트 확인이 필요한 경우에만 지정',
  ]);
});
