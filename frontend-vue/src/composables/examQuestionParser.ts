export interface ParsedExamQuestion {
  orderIndex: number
  text: string
  type: 'TEXT'
  correctAnswer: string
  maxScore: number
}

export function parseExamQuestionsText(content: string): ParsedExamQuestion[] {
  const lines = content
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)

  const questions: ParsedExamQuestion[] = []
  for (let index = 0; index < lines.length; index += 4) {
    const block = lines.slice(index, index + 4)
    if (block.length < 4) {
      throw new Error(`Задание ${questions.length + 1}: неполный блок из 4 строк`)
    }

    const header = parseQuestionHeader(block[0], questions.length + 1)
    const text = parseRequiredField(block[1], 'Текст задания')
    const correctAnswer = parseRequiredField(block[2], 'Правильный ответ')
    const maxScoreText = parseRequiredField(block[3], 'Балл за правильный ответ')
    const maxScore = Number(maxScoreText.replace(',', '.'))
    if (!Number.isFinite(maxScore) || maxScore <= 0) {
      throw new Error(`Задание ${header.orderIndex}: балл должен быть положительным числом`)
    }

    questions.push({
      orderIndex: header.orderIndex,
      text: `${header.title}\n${text}`,
      type: 'TEXT',
      correctAnswer,
      maxScore
    })
  }

  return questions
}

function parseQuestionHeader(line: string, expectedIndex: number) {
  const match = line.match(/^(\d+)\.\s*Название задания:\s*(.+)$/i)
  if (!match) {
    throw new Error(`Задание ${expectedIndex}: первая строка должна быть "N. Название задания: ..."`)
  }

  const orderIndex = Number(match[1])
  if (orderIndex !== expectedIndex) {
    throw new Error(`Ожидалось задание ${expectedIndex}, найдено задание ${orderIndex}`)
  }

  return {
    orderIndex,
    title: match[2].trim()
  }
}

function parseRequiredField(line: string, label: string) {
  const prefix = `${label}:`
  if (!line.toLowerCase().startsWith(prefix.toLowerCase())) {
    throw new Error(`Ожидалась строка "${prefix} ..."`)
  }

  const value = line.slice(prefix.length).trim()
  if (!value) {
    throw new Error(`Поле "${label}" не заполнено`)
  }
  return value
}
