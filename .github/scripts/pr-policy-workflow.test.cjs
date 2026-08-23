const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');

const workflow = fs.readFileSync(path.join(__dirname, '../workflows/pr-policy.yml'), 'utf8');

test('PR 정책 검증은 신뢰된 base SHA의 workflow와 모듈을 사용한다', () => {
  assert.match(workflow, /\r?\n  pull_request_target:\r?\n/);
  assert.match(
    workflow,
    /uses: actions\/checkout@v4\s+with:\s+ref: \$\{\{ github\.event\.pull_request\.base\.sha \}\}/,
  );
});

test('PR 정책 검증은 head와 base 저장소 식별자를 정책 모듈에 전달한다', () => {
  assert.match(workflow, /headRepo: context\.payload\.pull_request\.head\.repo\.full_name/);
  assert.match(workflow, /baseRepo: context\.payload\.pull_request\.base\.repo\.full_name/);
});
