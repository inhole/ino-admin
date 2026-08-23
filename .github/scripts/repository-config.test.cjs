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
