import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join } from 'node:path'
import en from '../src/i18n/locales/en-US.json'
import de from '../src/i18n/locales/de-DE.json'
import { LOCALES } from '../src/i18n'

type Tree = { [k: string]: string | Tree }
const keysOf = (o: Tree, p = ''): string[] =>
  Object.entries(o).flatMap(([k, v]) => (typeof v === 'string' ? [p + k] : keysOf(v, `${p}${k}.`)))

/** Keys present in `from` but missing in `to`. */
export const missingKeys = (from: Tree, to: Tree) => {
  const have = new Set(keysOf(to))
  return keysOf(from).filter((k) => !have.has(k))
}

test('English and German have exactly the same keys', () => {
  expect(missingKeys(en, de), 'keys missing from de-DE').toEqual([])
  expect(missingKeys(de, en), 'keys missing from en-US').toEqual([])
})

// Every translation key the source uses must exist in both languages.
// Plural keys are used without their suffix, so `x` is fine if `x_one` and `x_other` exist.
test('every key used in src exists in both languages', () => {
  const files = (dir: string): string[] =>
    readdirSync(dir).flatMap((f) => {
      const p = join(dir, f)
      return statSync(p).isDirectory() ? files(p) : /\.tsx?$/.test(f) ? [p] : []
    })
  const namespaces = Object.keys(en).join('|')
  const keyLiteral = new RegExp(`['"\`]((?:${namespaces})\\.[A-Za-z_.]+)['"\`]`, 'g')
  const used = new Set(files('src').flatMap((f) => [...readFileSync(f, 'utf8').matchAll(keyLiteral)].map((m) => m[1])))
  // The language names are looked up as `language.${locale}`.
  LOCALES.forEach((l) => used.add(`language.${l}`))

  for (const [name, tree] of [['en-US', en], ['de-DE', de]] as const) {
    const have = new Set(keysOf(tree))
    const missing = [...used].filter((k) => !have.has(k) && !(have.has(`${k}_one`) && have.has(`${k}_other`)))
    expect(missing, `keys used in src but missing from ${name}`).toEqual([])
  }
  expect(used.size).toBeGreaterThan(10)
})

test('the check reports a key removed from one language', () => {
  const broken = structuredClone(de) as Tree
  delete (broken.reserve as Tree).failed
  expect(missingKeys(en, broken)).toEqual(['reserve.failed'])
})
