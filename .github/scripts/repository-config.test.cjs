const { readFileSync } = require('node:fs');
const { test } = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');

const repositoryRoot = path.resolve(__dirname, '..', '..');
const issueTemplateDirectory = path.join(repositoryRoot, '.github', 'ISSUE_TEMPLATE');

function readIssueForm(fileName) {
  return readFileSync(path.join(issueTemplateDirectory, fileName), 'utf8');
}

function assertIncludesAll(content, values) {
  for (const value of values) {
    assert.ok(content.includes(value), `expected form to include ${value}`);
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
});
