const assert = require('node:assert/strict');
const test = require('node:test');

const { validatePullRequest } = require('./pr-policy.cjs');

test('일반 작업 브랜치는 main을 직접 대상으로 할 수 없다', () => {
  const errors = validatePullRequest({
    head: 'feat/123-user-search',
    base: 'main',
    title: 'feat: 사용자 검색 추가',
    body: 'Closes #123',
    issues: [{ number: 123, milestoneNumber: 7 }],
  });

  assert.ok(errors.some((error) => error.includes("main 대상 PR의 head는 'dev'")));
});

test('dev 대상 PR은 이슈 참조가 필요하다', () => {
  const errors = validatePullRequest({
    head: 'feat/123-user-search',
    base: 'dev',
    title: 'feat: 사용자 검색 추가',
    body: '검색 기능을 추가합니다.',
    issues: [],
  });

  assert.ok(errors.some((error) => error.includes('이슈 참조')));
});

test('dev에서 main으로 가는 배치 PR은 같은 Milestone의 이슈만 닫는다', () => {
  const errors = validatePullRequest({
    head: 'dev',
    base: 'main',
    headRepo: 'inhole/ino-admin',
    baseRepo: 'inhole/ino-admin',
    title: 'feat: 사용자 관리 배치 전달',
    body: 'Closes #123\nCloses #124',
    issues: [
      { number: 123, milestoneNumber: 7 },
      { number: 124, milestoneNumber: 8 },
    ],
  });

  assert.ok(errors.some((error) => error.includes('같은 Milestone')));
});

test('fork 저장소의 dev 브랜치는 main 배치 PR을 만들 수 없다', () => {
  const errors = validatePullRequest({
    head: 'dev',
    base: 'main',
    headRepo: 'attacker/ino-admin',
    baseRepo: 'inhole/ino-admin',
    title: 'feat: 사용자 관리 배치 전달',
    body: 'Closes #123',
    issues: [{ number: 123, milestoneNumber: 7 }],
  });

  assert.ok(errors.some((error) => error.includes('같은 저장소')));
});

test('PR 제목의 변경사항에는 한글이 포함되어야 한다', () => {
  const errors = validatePullRequest({
    head: 'feat/123-user-search',
    base: 'dev',
    title: 'feat: add user search',
    body: 'Refs: #123',
    issues: [{ number: 123, milestoneNumber: 7 }],
  });

  assert.ok(errors.some((error) => error.includes('한글')));
});

test('올바른 dev 배치 PR을 허용한다', () => {
  const errors = validatePullRequest({
    head: 'dev',
    base: 'main',
    headRepo: 'inhole/ino-admin',
    baseRepo: 'inhole/ino-admin',
    title: 'feat: 사용자 관리 배치 전달',
    body: 'Closes #123\nCloses #124',
    issues: [
      { number: 123, milestoneNumber: 7 },
      { number: 124, milestoneNumber: 7 },
    ],
  });

  assert.deepEqual(errors, []);
});
