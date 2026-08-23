const BRANCH_PATTERN = /^(feat|fix|docs|test|refactor|perf|style|build|ci|chore|revert)\/\d+-[a-z0-9][a-z0-9-]*$/;
const CODEX_PATTERN = /^codex\/\d+-[a-z0-9][a-z0-9-]*$/;
const TITLE_PATTERN = /^(feat|fix|docs|test|refactor|perf|style|build|ci|chore|revert): .+$/;

function extractIssueReferences(body = '') {
  const collect = (keyword) => [...body.matchAll(new RegExp(`\\b(?:${keyword})\\s*:?[ \\t]*#(\\d+)`, 'gi'))]
    .map((match) => Number(match[1]));
  return {
    refs: [...new Set(collect('refs?'))],
    closes: [...new Set(collect('closes?|fixes?|resolves?'))],
  };
}

function validatePullRequest(input) {
  const { head, base, title, body, issues } = input;
  const errors = [];
  const isWorkBranch = BRANCH_PATTERN.test(head) || CODEX_PATTERN.test(head);

  if (!TITLE_PATTERN.test(title)) {
    errors.push(`PR 제목 '${title}'은 'type: 변경사항' 형식이어야 합니다.`);
  }

  if (base === 'main' && head !== 'dev') {
    errors.push("main 대상 PR의 head는 'dev'여야 합니다.");
  }
  if (base === 'dev' && !isWorkBranch) {
    errors.push("dev 대상 PR의 head는 이슈 번호가 포함된 작업 브랜치여야 합니다.");
  }
  if (base !== 'dev' && base !== 'main') {
    errors.push("PR의 base는 'dev' 또는 'main'이어야 합니다.");
  }

  const references = extractIssueReferences(body);
  const numbers = [...new Set([...references.refs, ...references.closes])];

  if (base === 'dev' && isWorkBranch && numbers.length === 0) {
    errors.push('dev 대상 PR에는 이슈 참조가 필요합니다.');
  }

  if (head === 'dev' && base === 'main') {
    if (references.closes.length === 0) {
      errors.push('dev에서 main으로 가는 배치 PR에는 닫을 이슈 참조가 필요합니다.');
    }

    const issuesByNumber = new Map(issues.map((issue) => [issue.number, issue]));
    const referencedIssues = numbers.map((number) => issuesByNumber.get(number));
    const missingIssueNumbers = numbers.filter((number) => !issuesByNumber.has(number));

    if (missingIssueNumbers.length > 0) {
      errors.push(`참조 이슈 정보를 찾을 수 없습니다: ${missingIssueNumbers.map((number) => `#${number}`).join(', ')}.`);
    }

    if (referencedIssues.every(Boolean)) {
      const milestoneNumbers = referencedIssues.map((issue) => issue.milestoneNumber);
      if (milestoneNumbers.some((milestoneNumber) => milestoneNumber === null)) {
        errors.push('배치 PR의 참조 이슈에는 Milestone이 필요합니다.');
      } else if (new Set(milestoneNumbers).size > 1) {
        errors.push('배치 PR의 참조 이슈는 같은 Milestone에 속해야 합니다.');
      }
    }
  }

  return errors;
}

module.exports = { extractIssueReferences, validatePullRequest };
