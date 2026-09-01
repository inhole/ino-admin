const assert = require('node:assert/strict');
const { mkdtempSync, mkdirSync, readFileSync, writeFileSync } = require('node:fs');
const { tmpdir } = require('node:os');
const path = require('node:path');
const { test } = require('node:test');

const { collectJUnitResults, renderMarkdown } = require('./test-summary.cjs');

test('JUnit XML의 실행·성공·실패·건너뜀 수를 집계한다', () => {
  const root = mkdtempSync(path.join(tmpdir(), 'ino-admin-test-summary-'));
  const resultDirectory = path.join(root, 'build', 'test-results', 'test');
  mkdirSync(resultDirectory, { recursive: true });
  writeFileSync(
    path.join(resultDirectory, 'TEST-example.xml'),
    `<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="ExampleTest" tests="4" skipped="1" failures="1" errors="0" time="1.25">
  <testcase name="passes" classname="ExampleTest" time="0.1" />
  <testcase name="fails" classname="ExampleTest" time="0.2"><failure message="expected true" /></testcase>
  <testcase name="skips" classname="ExampleTest"><skipped /></testcase>
  <testcase name="also passes" classname="ExampleTest" time="0.3" />
</testsuite>`,
  );

  const result = collectJUnitResults(root, ['build/test-results']);

  assert.deepEqual(
    {
      tests: result.tests,
      passed: result.passed,
      failures: result.failures,
      errors: result.errors,
      skipped: result.skipped,
      durationSeconds: result.durationSeconds,
    },
    { tests: 4, passed: 2, failures: 1, errors: 0, skipped: 1, durationSeconds: 1.25 },
  );
  assert.deepEqual(result.failedTests, ['ExampleTest > fails']);
});

test('여러 리포트를 합산하고 중복 include 경로에서도 한 번만 센다', () => {
  const root = mkdtempSync(path.join(tmpdir(), 'ino-admin-test-summary-'));
  const resultDirectory = path.join(root, 'test-results', 'vitest');
  mkdirSync(resultDirectory, { recursive: true });
  writeFileSync(
    path.join(resultDirectory, 'results.xml'),
    '<testsuites><testsuite name="one" tests="2" failures="0" errors="0" skipped="0" time="0.5" /></testsuites>',
  );

  const result = collectJUnitResults(root, ['test-results', 'test-results/vitest']);

  assert.equal(result.files, 1);
  assert.equal(result.tests, 2);
  assert.equal(result.passed, 2);
});

test('Markdown에는 테스트 표, 실패 목록, TDD 범위 안내가 포함된다', () => {
  const markdown = renderMarkdown('프론트엔드 단위 테스트', {
    files: 1,
    tests: 3,
    passed: 1,
    failures: 1,
    errors: 0,
    skipped: 1,
    durationSeconds: 0.75,
    failedTests: ['UsersPage > 권한 오류를 표시한다'],
  }, 'abc123');

  assert.match(markdown, /\| 실행 \| 성공 \| 실패 \| 오류 \| 건너뜀 \| 소요 시간 \|/);
  assert.match(markdown, /\| 3 \| 1 \| 1 \| 0 \| 1 \| 0\.75초 \|/);
  assert.match(markdown, /UsersPage > 권한 오류를 표시한다/);
  assert.match(markdown, /RED→GREEN 작성 순서가 아닌 현재 자동 테스트 결과/);
  assert.match(markdown, /abc123/);
});

test('리포트가 없으면 누락을 성공으로 오인하지 않도록 경고한다', () => {
  const root = mkdtempSync(path.join(tmpdir(), 'ino-admin-test-summary-'));
  const result = collectJUnitResults(root, ['build/test-results']);
  const markdown = renderMarkdown('백엔드 테스트', result, 'abc123');

  assert.equal(result.files, 0);
  assert.match(markdown, /JUnit XML 리포트를 찾지 못했습니다/);
});
