const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');

const workflow = fs.readFileSync(path.join(__dirname, '../workflows/pr-policy.yml'), 'utf8');

test('PR 정책 검증은 신뢰된 base SHA의 workflow와 모듈을 사용한다', () => {
  assert.match(workflow, /\n  pull_request_target:\n/);
  assert.match(
    workflow,
    /uses: actions\/checkout@v4\s+with:\s+ref: \$\{\{ github\.event\.pull_request\.base\.sha \}\}/,
  );
});
