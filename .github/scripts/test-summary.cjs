const { appendFileSync, existsSync, readdirSync, readFileSync } = require('node:fs');
const path = require('node:path');

function parseArguments(argv) {
  const options = { includes: [] };
  for (let index = 0; index < argv.length; index += 1) {
    const value = argv[index];
    const next = argv[index + 1];
    if (value === '--include' && next) {
      options.includes.push(next.replaceAll('\\', '/'));
      index += 1;
    } else if (value === '--title' && next) {
      options.title = next;
      index += 1;
    } else if (value === '--status' && next) {
      options.status = next;
      index += 1;
    } else if (value === '--commit' && next) {
      options.commit = next;
      index += 1;
    }
  }
  return options;
}

function findXmlFiles(root, includes) {
  const files = new Set();
  const normalizedIncludes = includes.map((value) => value.replace(/^\.\//, '').replace(/\/$/, ''));

  function visit(directory) {
    for (const entry of readdirSync(directory, { withFileTypes: true })) {
      if (entry.name === '.git' || entry.name === 'node_modules') continue;
      const absolutePath = path.join(directory, entry.name);
      if (entry.isDirectory()) {
        visit(absolutePath);
      } else if (entry.isFile() && entry.name.endsWith('.xml')) {
        const relativePath = path.relative(root, absolutePath).replaceAll('\\', '/');
        if (normalizedIncludes.some((include) => relativePath.includes(include))) {
          files.add(absolutePath);
        }
      }
    }
  }

  if (existsSync(root)) visit(root);
  return [...files].sort();
}

function attribute(attributes, name) {
  const match = attributes.match(new RegExp(`(?:^|\\s)${name}="([^"]*)"`));
  return match?.[1] ?? '';
}

function numberAttribute(attributes, name) {
  const value = Number(attribute(attributes, name));
  return Number.isFinite(value) ? value : 0;
}

function decodeXml(value) {
  return value
    .replaceAll('&quot;', '"')
    .replaceAll('&apos;', "'")
    .replaceAll('&lt;', '<')
    .replaceAll('&gt;', '>')
    .replaceAll('&amp;', '&');
}

function collectJUnitResults(root, includes) {
  const xmlFiles = findXmlFiles(root, includes);
  const result = {
    files: xmlFiles.length,
    tests: 0,
    passed: 0,
    failures: 0,
    errors: 0,
    skipped: 0,
    durationSeconds: 0,
    failedTests: [],
  };

  for (const file of xmlFiles) {
    const xml = readFileSync(file, 'utf8');
    for (const match of xml.matchAll(/<testsuite\b([^>]*)>/g)) {
      const attributes = match[1];
      result.tests += numberAttribute(attributes, 'tests');
      result.failures += numberAttribute(attributes, 'failures');
      result.errors += numberAttribute(attributes, 'errors');
      result.skipped += numberAttribute(attributes, 'skipped');
      result.durationSeconds += numberAttribute(attributes, 'time');
    }

    for (const match of xml.matchAll(/<testcase\b([^>]*?)(?<!\/)>([\s\S]*?)<\/testcase>/g)) {
      if (!/<(?:failure|error)\b/.test(match[2])) continue;
      const className = decodeXml(attribute(match[1], 'classname'));
      const name = decodeXml(attribute(match[1], 'name'));
      result.failedTests.push([className, name].filter(Boolean).join(' > '));
    }
  }

  result.passed = Math.max(0, result.tests - result.failures - result.errors - result.skipped);
  result.durationSeconds = Number(result.durationSeconds.toFixed(3));
  return result;
}

function escapeMarkdown(value) {
  return String(value).replaceAll('|', '\\|').replaceAll('\n', ' ');
}

function translateStatus(status, result) {
  const statuses = { success: '성공', failure: '실패', cancelled: '취소' };
  if (status && statuses[status]) return statuses[status];
  if (result.failures > 0 || result.errors > 0) return '실패';
  return result.files > 0 ? '성공' : '확인 필요';
}

function renderMarkdown(title, result, commit, status) {
  const lines = [
    `## ${title} 자동 테스트 결과`,
    '',
    `- 전체 결과: **${translateStatus(status, result)}**`,
  ];
  if (commit) lines.push(`- 커밋: \`${escapeMarkdown(commit)}\``);
  lines.push(`- JUnit 리포트: ${result.files}개`, '');

  if (result.files === 0) {
    lines.push('> ⚠️ JUnit XML 리포트를 찾지 못했습니다. 테스트 실행 여부와 reporter 설정을 확인하세요.', '');
  } else {
    lines.push(
      '| 실행 | 성공 | 실패 | 오류 | 건너뜀 | 소요 시간 |',
      '| ---: | ---: | ---: | ---: | ---: | ---: |',
      `| ${result.tests} | ${result.passed} | ${result.failures} | ${result.errors} | ${result.skipped} | ${result.durationSeconds}초 |`,
      '',
    );
  }

  if (result.failedTests.length > 0) {
    lines.push('### 실패한 테스트', '');
    for (const failedTest of result.failedTests.slice(0, 20)) {
      lines.push(`- ${escapeMarkdown(failedTest)}`);
    }
    if (result.failedTests.length > 20) {
      lines.push(`- 그 외 ${result.failedTests.length - 20}개`);
    }
    lines.push('');
  }

  lines.push('> CI는 RED→GREEN 작성 순서가 아닌 현재 자동 테스트 결과를 검증합니다.', '');
  return lines.join('\n');
}

if (require.main === module) {
  const options = parseArguments(process.argv.slice(2));
  if (!options.title || options.includes.length === 0) {
    console.error('Usage: node test-summary.cjs --title <title> --include <path-fragment> [--include ...]');
    process.exitCode = 2;
  } else {
    const result = collectJUnitResults(process.cwd(), options.includes);
    const markdown = renderMarkdown(options.title, result, options.commit, options.status);
    if (process.env.GITHUB_STEP_SUMMARY) {
      appendFileSync(process.env.GITHUB_STEP_SUMMARY, `${markdown}\n`, 'utf8');
    } else {
      process.stdout.write(`${markdown}\n`);
    }
  }
}

module.exports = { collectJUnitResults, renderMarkdown };
