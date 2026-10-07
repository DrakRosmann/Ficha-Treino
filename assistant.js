// Assistente de treino: monta opções de programa a partir do perfil da pessoa.
// Dois motores com a mesma saída:
//   1. Regras de treino (offline e grátis) — divisões, padrões de movimento e restrições.
//   2. IA (Claude) — usa a chave da API da Anthropic que a pessoa salva no aparelho.
// Carregado antes do app.js; as funções daqui usam utilitários do app.js só quando são chamadas.
'use strict';

const ASST_OPTS = {
  objetivo: [['massa', 'Ganhar massa'], ['definir', 'Definição'], ['emagrecer', 'Perder gordura'], ['forca', 'Ganhar força'], ['condicionamento', 'Condicionamento'], ['saude', 'Saúde e bem-estar']],
  nivel: [['iniciante', 'Iniciante'], ['intermediario', 'Intermediário'], ['avancado', 'Avançado']],
  dias: [[2, '2'], [3, '3'], [4, '4'], [5, '5'], [6, '6']],
  tempo: [[30, '30'], [45, '45'], [60, '60'], [75, '75'], [90, '90']],
  local: [['academia', 'Academia'], ['casa_halteres', 'Casa com halteres'], ['casa', 'Casa sem equipamento']],
  sexo: [['f', 'Feminino'], ['m', 'Masculino'], ['', 'Não informar']],
  foco: [['peito', 'Peito'], ['costas', 'Costas'], ['ombros', 'Ombros'], ['bracos', 'Braços'], ['pernas', 'Pernas'], ['gluteos', 'Glúteos'], ['abdomen', 'Abdômen']],
  restr: [['joelho', 'Joelho'], ['lombar', 'Lombar / coluna'], ['ombro', 'Ombro'], ['punho', 'Punho / cotovelo'], ['quadril', 'Quadril'], ['impacto', 'Sem impacto (saltos)'], ['pressao', 'Pressão alta / coração']]
};
const ASST_DEFAULT = {
  objetivo: 'massa', nivel: 'iniciante', dias: 3, tempo: 60, local: 'academia',
  sexo: '', idade: '', peso: '', altura: '', foco: [], restr: [], obs: ''
};
const ASST_LEVEL_HINT = { iniciante: 'menos de 6 meses', intermediario: '6 meses a 2 anos', avancado: 'mais de 2 anos' };

/* ---------- Motor de regras ---------- */
// Candidatos por padrão de movimento, em ordem de preferência: [id, ambientes, restrições]
// Ambientes: g = academia, d = casa com halteres, b = só peso do corpo.
// Restrições: K joelho, L lombar, S ombro, W punho/cotovelo, H quadril, I impacto,
//             A evita para iniciantes, X só para avançados.
const ASST_SLOTS = {
  squat: [['agachamento', 'g', 'KL'], ['leg-press-45', 'g', ''], ['hack', 'g', 'K'], ['agachamento-smith', 'g', 'KL'], ['goblet', 'gd', 'K'],
    ['leg-press-horizontal', 'g', ''], ['agachamento-com-halteres', 'd', 'K'], ['agachamento-sumo-com-halter', 'd', 'H'],
    ['agachamento-com-peso-corporal', 'b', 'K'], ['agachamento-frontal', 'g', 'KLA']],
  lunge: [['bulgaro', 'gd', 'KH'], ['passada', 'gd', 'K'], ['afundo-reverso-com-halteres', 'gd', 'K'], ['leg-press-unilateral', 'g', ''],
    ['step-up', 'gd', 'K'], ['passada-com-peso-corporal', 'b', 'K']],
  hinge: [['terra-romeno', 'g', 'L'], ['stiff-halter', 'gd', 'L'], ['stiff-barra', 'g', 'L'], ['terra', 'g', 'LA'],
    ['levantamento-terra-com-barra-hexagonal', 'g', 'L'], ['stiff-unilateral', 'd', 'L'], ['ponte-de-gluteo-unilateral', 'b', '']],
  ham_curl: [['mesa-flexora', 'g', ''], ['cadeira-flexora', 'g', ''], ['flexora-em-pe', 'g', ''], ['ponte-de-gluteo-unilateral', 'db', '']],
  quad_iso: [['extensora', 'g', 'K'], ['cadeira-extensora-unilateral', 'g', 'K'], ['cadeirinha', 'db', 'K']],
  glute: [['hip-thrust', 'g', ''], ['elevacao-pelvica-maquina', 'g', ''], ['gluteo-polia', 'g', ''], ['abdutora', 'g', ''],
    ['ponte-de-gluteo-com-barra', 'g', ''], ['ponte-gluteo', 'db', ''], ['ponte-de-gluteo-unilateral', 'db', ''], ['gluteo-coice-no-solo-quatro-apoios', 'db', 'W']],
  calf: [['panturrilha-em-pe', 'g', ''], ['panturrilha-sentado', 'g', ''], ['panturrilha-leg', 'g', ''], ['panturrilha-smith', 'g', ''],
    ['panturrilha-em-pe-com-halteres', 'd', ''], ['panturrilha-unilateral-apoiado-no-halter', 'd', '']],
  h_push: [['supino-reto-barra', 'g', 'SW'], ['supino-reto-halter', 'gd', 'S'], ['supino-maquina', 'g', ''], ['supino-articulado', 'g', ''],
    ['supino-reto-com-halteres-pegada-neutra', 'gd', ''], ['supino-no-chao-com-halteres', 'd', ''], ['flexao', 'db', 'WA'], ['flexao-joelhos', 'b', 'W']],
  incline_push: [['supino-inclinado-halter', 'gd', 'S'], ['supino-inclinado-barra', 'g', 'SW'], ['supino-inclinado-articulado', 'g', ''],
    ['supino-inclinado-smith', 'g', 'S'], ['supino-inclinado-com-halteres-pegada-neutra', 'gd', ''], ['flexao-de-braco-com-pes-elevados', 'b', 'WSA']],
  chest_iso: [['peck-deck', 'g', ''], ['crossover-alto', 'g', ''], ['crossover-baixo', 'g', ''], ['crucifixo-reto', 'gd', 'S'], ['crucifixo-inclinado', 'gd', 'S']],
  v_push: [['desenvolvimento-halter', 'gd', 'S'], ['desenvolvimento-maquina', 'g', 'S'], ['arnold', 'gd', 'S'], ['desenvolvimento-barra', 'g', 'SLA'],
    ['desenvolvimento-articulado', 'g', 'S'], ['desenvolvimento-em-pe-com-halteres-pegada-neutra', 'd', 'S'], ['flexao-em-parada-de-mao-handstand-push-up', 'b', 'SWX']],
  lat_raise: [['elevacao-lateral', 'gd', ''], ['elevacao-lateral-polia', 'g', ''], ['elevacao-lateral-maquina', 'g', ''], ['elevacao-lateral-sentado-com-halteres', 'gd', '']],
  rear_delt: [['face-pull', 'g', ''], ['crucifixo-invertido', 'gd', ''], ['voador-inverso', 'g', ''], ['crucifixo-invertido-na-polia', 'g', '']],
  v_pull: [['puxada-aberta', 'g', ''], ['puxada-triangulo', 'g', ''], ['puxada-supinada', 'g', ''], ['barra-fixa', 'g', 'SA'], ['puxada-articulada', 'g', ''],
    ['chin-up', 'g', 'SA'], ['puxada-unilateral-na-polia', 'g', ''], ['pullover', 'd', 'S'], ['remada-invertida-remada-australiana', 'b', '']],
  h_pull: [['remada-curvada', 'g', 'LA'], ['remada-baixa', 'g', ''], ['serrote', 'gd', ''], ['remada-maquina', 'g', ''], ['remada-cavalinho', 'g', 'L'],
    ['remada-cavalinho-apoiada-maquina', 'g', ''], ['remada-com-halteres-apoiado-no-banco-inclinado', 'gd', ''], ['remada-curvada-com-halteres', 'd', 'L'],
    ['remada-sentado-unilateral-na-polia', 'g', ''], ['remada-invertida-remada-australiana', 'b', '']],
  lat_iso: [['pulldown', 'g', ''], ['pullover', 'gd', 'S'], ['pulldown-com-corda-bracos-estendidos', 'g', '']],
  shrug: [['encolhimento-halter', 'gd', ''], ['encolhimento-barra', 'g', '']],
  biceps: [['rosca-direta', 'g', 'W'], ['rosca-alternada', 'gd', ''], ['rosca-martelo', 'gd', ''], ['rosca-scott', 'g', 'W'], ['rosca-polia', 'g', ''],
    ['rosca-inclinada', 'gd', ''], ['rosca-w', 'g', ''], ['rosca-concentrada', 'gd', ''], ['rosca-maquina', 'g', '']],
  triceps: [['triceps-pulley', 'g', ''], ['triceps-corda', 'g', ''], ['triceps-frances', 'gd', 'SW'], ['triceps-testa', 'g', 'W'], ['triceps-coice', 'gd', ''],
    ['triceps-unilateral', 'g', ''], ['triceps-testa-com-halteres', 'd', 'W'], ['mergulho-banco', 'db', 'SW'], ['flexao-de-braco-fechada-diamante', 'b', 'WA']],
  core: [['prancha', 'gdb', ''], ['abdominal-infra', 'gdb', 'L'], ['abdominal-supra', 'gdb', 'L'], ['dead-bug-inseto-morto', 'gdb', ''], ['abdominal-polia', 'g', 'L'],
    ['elevacao-pernas', 'g', 'LSA'], ['prancha-lateral', 'gdb', ''], ['pallof-press', 'g', ''], ['abdominal-bicicleta', 'gdb', 'L']],
  cardio: [['bicicleta', 'g', ''], ['eliptico', 'g', ''], ['esteira', 'g', ''], ['escada', 'g', 'K'], ['remo-ergometrico', 'g', 'L'],
    ['corrida-caminhada-ao-ar-livre', 'db', ''], ['pular-corda', 'db', 'IK']],
  finisher: [['kettlebell-swing', 'g', 'L'], ['burpee', 'gdb', 'IKW'], ['escalador-mountain-climber', 'gdb', 'W'], ['polichinelo', 'gdb', 'I'],
    ['corda-naval-battle-rope', 'g', ''], ['agachamento-com-salto', 'gdb', 'IK']],
  mobility: [['melhor-alongamento-do-mundo-world-s-greatest-stretch', 'gdb', ''], ['alongamento-do-gato-cat-stretch', 'gdb', ''],
    ['circulos-de-quadril-em-pe', 'gdb', ''], ['inchworm-caminhada-da-lagarta', 'gdb', '']]
};
// Se um padrão não tiver opção segura/disponível, tenta estes no lugar
const ASST_FALLBACK = {
  squat: ['glute', 'quad_iso'], lunge: ['glute', 'ham_curl'], hinge: ['glute', 'ham_curl'],
  h_push: ['chest_iso'], incline_push: ['chest_iso', 'h_push'], v_push: ['lat_raise'],
  v_pull: ['h_pull', 'lat_iso'], h_pull: ['v_pull'], quad_iso: ['glute']
};
const ASST_COMPOUND = new Set(['squat', 'lunge', 'hinge', 'h_push', 'incline_push', 'v_push', 'v_pull', 'h_pull']);
const ASST_SLOT_KIND = { squat: 'leg', lunge: 'leg', hinge: 'leg', quad_iso: 'leg', ham_curl: 'leg', glute: 'leg', calf: 'leg',
  h_push: 'push', incline_push: 'push', chest_iso: 'push', v_push: 'push', lat_raise: 'push', triceps: 'push',
  v_pull: 'pull', h_pull: 'pull', lat_iso: 'pull', rear_delt: 'pull', biceps: 'pull', shrug: 'pull' };

// Fichas: padrões na ordem; ":2"/":3" = prioridade menor (sai primeiro se faltar tempo)
const ASST_SESSIONS = {
  fbA: ['Corpo inteiro A', 'squat h_push v_pull hinge:2 lat_raise:2 core:2 biceps:3 triceps:3'],
  fbB: ['Corpo inteiro B', 'hinge incline_push h_pull v_push:2 lunge:2 core:2 calf:3 triceps:3'],
  fbC: ['Corpo inteiro C', 'lunge h_push v_pull v_push:2 glute:2 core:2 biceps:3 rear_delt:3'],
  upA: ['Superiores A', 'h_push h_pull v_push v_pull incline_push:2 lat_raise:2 biceps:3 triceps:3'],
  loA: ['Inferiores A', 'squat hinge lunge:2 quad_iso:2 ham_curl:2 calf:3 core:3'],
  upB: ['Superiores B', 'incline_push v_pull h_pull v_push:2 chest_iso:2 rear_delt:2 biceps:3 triceps:3'],
  loB: ['Inferiores B', 'hinge squat glute:2 lunge:2 ham_curl:2 quad_iso:3 calf:3 core:3'],
  push: ['Push — peito, ombro e tríceps', 'h_push incline_push v_push chest_iso:2 lat_raise:2 triceps:2 triceps:3'],
  pull: ['Pull — costas e bíceps', 'v_pull h_pull h_pull:2 lat_iso:2 rear_delt:2 biceps:2 biceps:3 shrug:3'],
  legs: ['Legs — pernas', 'squat hinge lunge:2 quad_iso:2 ham_curl:2 calf:2 core:3'],
  abcA: ['A — Peito, ombro e tríceps', 'h_push incline_push v_push chest_iso:2 lat_raise:2 triceps:2 triceps:3'],
  abcB: ['B — Costas e bíceps', 'v_pull h_pull h_pull:2 lat_iso:2 rear_delt:2 biceps:2 biceps:3'],
  abcC: ['C — Pernas e abdômen', 'squat hinge quad_iso:2 ham_curl:2 lunge:2 calf:3 core:3'],
  abcdA: ['A — Peito e tríceps', 'h_push incline_push chest_iso:2 chest_iso:3 triceps:2 triceps:2'],
  abcdB: ['B — Costas e bíceps', 'v_pull h_pull h_pull:2 lat_iso:2 biceps:2 biceps:3'],
  abcdC: ['C — Pernas', 'squat hinge lunge:2 quad_iso:2 ham_curl:2 calf:2 glute:3'],
  abcdD: ['D — Ombros e abdômen', 'v_push lat_raise rear_delt:2 lat_raise:2 shrug:3 core:2 core:3'],
  e1: ['A — Peito', 'h_push incline_push chest_iso:2 chest_iso:2 incline_push:3'],
  e2: ['B — Costas', 'v_pull h_pull v_pull:2 h_pull:2 lat_iso:2 shrug:3'],
  e3: ['C — Pernas', 'squat hinge lunge:2 quad_iso:2 ham_curl:2 calf:2 glute:3'],
  e4: ['D — Ombros', 'v_push lat_raise v_push:2 lat_raise:2 rear_delt:2 shrug:3 core:3'],
  e5: ['E — Braços e abdômen', 'biceps triceps biceps:2 triceps:2 core:2 biceps:3 triceps:3'],
  cA: ['Circuito A', 'squat h_push h_pull lunge:2 v_push:2 core:2'],
  cB: ['Circuito B', 'hinge incline_push v_pull glute:2 lat_raise:2 core:2']
};
const ASST_SPLITS = {
  fb2: ['Corpo inteiro A/B', ['fbA', 'fbB'], 'Corpo inteiro em cada treino: cada músculo trabalha toda vez que você treina.'],
  fb3: ['Corpo inteiro A/B/C', ['fbA', 'fbB', 'fbC'], 'Corpo inteiro com 3 fichas diferentes: muita frequência por músculo e treinos curtos — ótimo para evoluir rápido e aprender os movimentos.'],
  ul2: ['Superiores / Inferiores', ['upA', 'loA'], 'Um dia para a parte de cima e outro para as pernas: treinos mais focados.'],
  ul4: ['Upper / Lower', ['upA', 'loA', 'upB', 'loB'], 'Superiores e inferiores alternados: cada músculo treina 2x por semana com bom volume — um dos formatos mais eficientes para 4 dias.'],
  ppl: ['Push / Pull / Legs', ['push', 'pull', 'legs'], 'Empurrar, puxar e pernas: junta os músculos que trabalham juntos. Com 6 dias, cada ficha se repete 2x na semana.'],
  abc: ['ABC', ['abcA', 'abcB', 'abcC'], 'O clássico das academias: cada ficha foca em poucos grupos, com bastante volume para cada um.'],
  abcd: ['ABCD', ['abcdA', 'abcdB', 'abcdC', 'abcdD'], 'Quatro fichas para dar mais atenção a cada grupo, incluindo ombros e abdômen.'],
  abcde: ['ABCDE', ['e1', 'e2', 'e3', 'e4', 'e5'], 'Um grupo por dia com muito volume — para quem já treina há bastante tempo.'],
  ulppl: ['Upper / Lower + PPL', ['upA', 'loA', 'push', 'pull', 'legs'], 'Mistura Upper/Lower com Push/Pull/Legs: cada músculo treina 2x por semana em 5 dias.'],
  ulfb: ['Superiores / Inferiores / Corpo inteiro', ['upA', 'loA', 'fbA'], 'Parte de cima, pernas e um corpo inteiro: cada músculo treina 2x por semana em só 3 dias.'],
  circ: ['Circuito corpo inteiro', ['cA', 'cB'], 'Circuitos de corpo inteiro com pouco descanso: mantém o coração acelerado e aumenta o gasto de calorias.']
};
const ASST_GOAL_WHY = {
  massa: 'Ganhar massa: 3–4 séries de 8–12 repetições, descanso de 60–90 s, terminando cada série perto da falha.',
  forca: 'Força: exercícios principais pesados (4–6 repetições) com descanso longo; os acessórios usam mais repetições.',
  definir: 'Definição: 10–15 repetições com descanso curto e cardio no fim, para gastar calorias sem perder músculo.',
  emagrecer: 'Perder gordura: treino de força para manter os músculos, descanso curto, um exercício intenso no fim e cardio.',
  condicionamento: 'Condicionamento: repetições altas, pouco descanso e um exercício metabólico para fechar o treino.',
  saude: 'Saúde: volume moderado, movimentos básicos, mobilidade no início e cardio leve — o mais importante é a constância.'
};
const ASST_RESTR_WHY = {
  joelho: 'joelho (sem agachamentos profundos, afundos e saltos)',
  lombar: 'lombar (sem levantamento terra e remadas sem apoio; abdômen com exercícios de estabilização)',
  ombro: 'ombro (sem desenvolvimentos e mergulhos; supino com pegada neutra ou máquina)',
  punho: 'punho e cotovelo (sem barra reta e flexões no chão)',
  quadril: 'quadril (sem afundos amplos e agachamento sumô)',
  impacto: 'sem impacto (sem saltos nem corrida)',
  pressao: 'pressão alta (descansos maiores e sem cargas máximas)'
};
const ASST_RESTR_TAG = { joelho: 'K', lombar: 'L', ombro: 'S', punho: 'W', quadril: 'H', impacto: 'I' };
// Dicas por restrição: [padrões em que vale a dica, texto] — aparece uma vez por ficha
const ASST_RESTR_NOTE = {
  joelho: [['squat lunge quad_iso', 'Joelho: amplitude confortável e carga moderada']],
  lombar: [['squat', 'Lombar: costas bem apoiadas, sem arredondar'], ['h_pull v_pull', 'Lombar: coluna neutra, sem jogar o tronco']],
  ombro: [['h_push incline_push v_push', 'Ombro: amplitude sem dor, cotovelos um pouco abaixo dos ombros'], ['lat_raise', 'Ombro: suba só até a altura dos ombros']],
  punho: [['h_push incline_push triceps', 'Punho: mantenha o punho firme e alinhado'], ['biceps h_pull v_pull', 'Punho: use pegada neutra se incomodar']],
  quadril: [['squat lunge glute', 'Quadril: amplitude confortável, sem dor']],
  pressao: [['squat hinge lunge h_push incline_push v_push h_pull v_pull', 'Respire durante o movimento, sem prender o ar']]
};
const ASST_EX_NOTE = {
  'remada-invertida-remada-australiana': 'Use uma barra baixa ou uma mesa bem firme',
  'esteira': 'Caminhada inclinada ou corrida leve',
  'flexao-joelhos': 'Quando ficar fácil, passe para a flexão completa',
  'mergulho-banco': 'Use um banco ou uma cadeira firme'
};
const ASST_HEAVY = new Set(['squat', 'hinge', 'h_push', 'incline_push', 'v_push', 'v_pull', 'h_pull']);
const ASST_WEEK = { 2: [1, 4], 3: [1, 3, 5], 4: [1, 2, 4, 5], 5: [1, 2, 3, 4, 5], 6: [1, 2, 3, 4, 5, 6] };

function asstProfile() { return { ...ASST_DEFAULT, ...(S.profile || {}) }; }
function asstBmi(p) {
  const w = num(p.peso), h = num(p.altura);
  if (!w || !h) return null;
  const m = h > 3 ? h / 100 : h;
  return w / (m * m);
}
function asstEnv(p) { return { academia: 'g', casa_halteres: 'd', casa: 'b' }[p.local] || 'g'; }
function asstAvoid(p) {
  const tags = new Set(p.restr.map(r => ASST_RESTR_TAG[r]).filter(Boolean));
  const bmi = asstBmi(p), age = num(p.idade);
  if (tags.has('K') || (bmi && bmi >= 30) || (age && age >= 55)) tags.add('I');
  if (p.nivel === 'iniciante') tags.add('A');
  if (p.nivel !== 'avancado') tags.add('X');
  return tags;
}
// Pseudo-aleatório determinístico (para "gerar outra variação")
function asstRand(seed) { let x = (seed * 9301 + 49297) % 233280; return () => (x = (x * 9301 + 49297) % 233280) / 233280; }

function asstSplitKeys(p) {
  const d = p.dias, lv = p.nivel, burn = p.objetivo === 'emagrecer' || p.objetivo === 'condicionamento';
  let list;
  if (d <= 2) list = burn ? ['circ', 'fb2', 'ul2'] : ['fb2', 'ul2', 'circ'];
  else if (d === 3) list = lv === 'iniciante' ? ['fb3', 'ulfb', 'ppl'] : burn ? ['fb3', 'circ', 'ppl'] : ['ppl', 'fb3', 'abc'];
  else if (d === 4) list = lv === 'iniciante' ? ['ul4', 'fb2', 'abcd'] : burn ? ['ul4', 'circ', 'fb2'] : ['ul4', 'abcd', 'ppl'];
  else if (d === 5) list = lv === 'avancado' ? ['ulppl', 'abcde', 'ul4'] : ['ulppl', 'ul4', 'abc'];
  else list = lv === 'iniciante' ? ['ppl', 'ul4', 'fb3'] : ['ppl', 'abc', 'ulppl'];
  // Só com o peso do corpo, divisões por grupo ficam pobres: prefere corpo inteiro e circuitos
  if (p.local === 'casa') list = [...new Set([...list.filter(k => ['fb2', 'fb3', 'circ', 'ul2', 'ul4', 'ulfb'].includes(k)), 'fb3', 'circ', 'fb2'])];
  return list.slice(0, 3);
}
function asstDays(n, d) {
  const week = ASST_WEEK[d] || ASST_WEEK[3];
  if (n === week.length) return week.map(x => [x]);
  if (week.length % n === 0) return Array.from({ length: n }, (_, i) => week.filter((_, j) => j % n === i));
  return null; // não divide certinho: treino em sequência, sem dia fixo
}
function asstScheme(p, role, cIdx, kind, cardioMin, circuit, slot) {
  const g = p.objetivo;
  const T = {
    forca: role === 'c' ? (cIdx < 2 && ASST_HEAVY.has(slot) ? [5, '4-6', 180] : [4, '6-8', 120]) : [3, '8-10', 90],
    massa: role === 'c' ? [4, '8-12', 90] : [3, '10-15', 60],
    definir: role === 'c' ? [3, '10-12', 75] : [3, '12-15', 45],
    emagrecer: role === 'c' ? [3, '12-15', 60] : [3, '15', 45],
    condicionamento: role === 'c' ? [3, '12-15', 45] : [3, '15-20', 30],
    saude: role === 'c' ? [3, '10-12', 75] : [2, '12-15', 60]
  };
  let [sets, reps, rest] = T[g] || T.massa;
  if (p.nivel === 'iniciante') {
    sets = Math.min(sets, 3);
    if (g === 'forca' && role === 'c') { reps = '6-8'; rest = 150; }
  }
  if (p.nivel === 'avancado' && role === 'c' && sets < 4 && g !== 'saude') sets++;
  if (p.restr.includes('pressao')) {
    rest = Math.max(rest, 60);
    if (g === 'forca') { sets = Math.min(sets, 3); reps = '8-10'; rest = 120; }
  }
  if (slot === 'core') { sets = Math.min(sets, 3); reps = '12-15'; rest = 45; }
  if (circuit) rest = 30;
  if (kind === 's') reps = p.nivel === 'iniciante' ? '20-30' : '30-45';
  if (kind === 'c') return [1, String(cardioMin), 0];
  return [sets, reps, rest];
}
const asstMinutes = (sets, rest, kind) => kind === 'c' ? 0 : sets * (rest + 40) / 60 + 1;

function asstPick(slot, p, env, avoid, used, offset, rnd) {
  const ok = (ASST_SLOTS[slot] || []).filter(([id, envs, tags]) => envs.includes(env) && ![...tags].some(t => avoid.has(t)) && !used.has(id) && getEx(id));
  if (!ok.length) return null;
  let list = ok;
  // Iniciante na academia: máquinas e polias primeiro (mais seguras para aprender)
  if (p.nivel === 'iniciante' && env === 'g' && (ASST_COMPOUND.has(slot) || ASST_SLOT_KIND[slot])) {
    const easy = e => ['Máquina', 'Polia', 'Smith'].includes(getEx(e[0]).equip) ? 0 : 1;
    list = ok.map((e, i) => [e, i]).sort((a, b) => easy(a[0]) - easy(b[0]) || a[1] - b[1]).map(x => x[0]);
  }
  const span = Math.min(list.length, 3);
  const i = (offset + Math.floor(rnd() * span)) % span;
  return list[i][0];
}

function asstBuildOption(p, key, seed) {
  const [splitName, sessionKeys, splitWhy] = ASST_SPLITS[key];
  const env = asstEnv(p), avoid = asstAvoid(p), rnd = asstRand(seed + key.length * 7);
  const goal = p.objetivo;
  const tempo = +p.tempo || 60;
  let cardioMin = { emagrecer: 20, condicionamento: 15, definir: 15, saude: 20 }[goal] || 0;
  if (tempo <= 30) cardioMin = goal === 'emagrecer' || goal === 'condicionamento' ? 10 : 0;
  const finisher = goal === 'emagrecer' || goal === 'condicionamento';
  const mobility = goal === 'saude' || p.nivel === 'iniciante' && num(p.idade) >= 50;
  const daysPlan = asstDays(sessionKeys.length, +p.dias);
  const focus = new Set(p.foco);
  let maxMin = 0;
  const sessions = sessionKeys.map((sk, k) => {
    const [name, spec] = ASST_SESSIONS[sk];
    const circuit = sk.startsWith('c');
    let slots = spec.split(' ').map((t, order) => { const [slot, pri] = t.split(':'); return { slot, pri: +(pri || 1), order }; });
    const has = list => slots.some(x => list.includes(x.slot));
    const chest = has(['h_push', 'incline_push']), pushC = chest || has(['v_push']), pullC = has(['v_pull', 'h_pull']);
    const leg = has(['squat', 'hinge', 'lunge', 'glute', 'quad_iso', 'ham_curl']);
    const add = (slot, pri) => slots.push({ slot, pri, order: 50 + slots.length });
    // Prioridades da pessoa: um exercício a mais para o grupo nas fichas em que ele já aparece
    if (focus.has('peito') && chest) add('chest_iso', 1);
    if (focus.has('costas') && pullC) add('lat_iso', 1);
    if (focus.has('ombros') && (pushC || pullC)) add('lat_raise', 1);
    if (focus.has('bracos')) { if (pullC) add('biceps', 1); if (pushC) add('triceps', 1); }
    if (focus.has('pernas') && leg) add('quad_iso', 1);
    if (focus.has('gluteos') && leg) { add('glute', 1); add('glute', 2); }
    if (focus.has('abdomen')) add('core', 1);
    const extra = (mobility ? 4 : 0) + (finisher ? 5 : 0) + 5; // aquecimento + extras
    let budget = tempo - cardioMin - extra;
    const used = new Set(), chosen = [];
    let cIdx = 0;
    for (const pri of [1, 2, 3]) {
      for (const s of slots.filter(x => x.pri === pri).sort((a, b) => a.order - b.order)) {
        if (chosen.length >= 9) break;
        let slot = s.slot, id = asstPick(slot, p, env, avoid, used, k, rnd);
        if (!id) for (const alt of ASST_FALLBACK[slot] || []) { id = asstPick(alt, p, env, avoid, used, k, rnd); if (id) { slot = alt; break; } }
        if (!id) continue;
        const ex = getEx(id), role = ASST_COMPOUND.has(slot) ? 'c' : 'i';
        const [sets, reps, rest] = asstScheme(p, role, role === 'c' ? cIdx : 9, ex.kind, cardioMin, circuit, slot);
        const cost = asstMinutes(sets, rest, ex.kind);
        if (chosen.length >= 3 && cost > budget) continue;
        budget -= cost;
        if (role === 'c') cIdx++;
        used.add(id);
        chosen.push({ id, slot, role, sets, reps, rest, order: s.order });
      }
    }
    // Ordem final: compostos, isolados, abdômen
    const rank = c => c.slot === 'core' ? 2 : c.role === 'c' ? 0 : 1;
    chosen.sort((a, b) => rank(a) - rank(b) || a.order - b.order);
    const noted = new Set();
    const items = chosen.map(c => [c.id, c.sets, c.reps, c.rest, asstNote(p, c, noted)]);
    if (circuit && items.length) items[0][4] = 'Circuito: faça um exercício após o outro e descanse 1–2 min ao fim de cada volta';
    if (mobility) { const m = asstPick('mobility', p, env, avoid, used, k, rnd); if (m) items.unshift([m, 2, '30', 0, 'Aquecimento']); }
    if (finisher) { const f = asstPick('finisher', p, env, avoid, used, k, rnd); if (f) items.push([f, 3, getEx(f).kind === 's' ? '30' : '15-20', 30, 'Final intenso']); }
    if (cardioMin) { const c = asstPick('cardio', p, env, avoid, used, k, rnd); if (c) items.push([c, 1, String(cardioMin), 0, avoid.has('I') ? 'Ritmo moderado, sem impacto' : 'Ritmo moderado']); }
    const minutes = Math.round(5 + sum(items.map(([id, sets, , rest]) => asstMinutes(sets, rest, getEx(id).kind))) + cardioMin);
    maxMin = Math.max(maxMin, minutes);
    return { name, days: daysPlan ? daysPlan[k] : [], items };
  });

  const why = [splitWhy, ASST_GOAL_WHY[goal]];
  if (p.nivel === 'iniciante') why.push('Iniciante: priorizei máquinas e exercícios simples. Foque na técnica e aumente a carga aos poucos.');
  const restr = p.restr.map(r => ASST_RESTR_WHY[r]).filter(Boolean);
  if (restr.length) why.push('Cuidados com: ' + restr.join('; ') + '.');
  const bmi = asstBmi(p), age = num(p.idade);
  if (!p.restr.includes('impacto') && !p.restr.includes('joelho') && ((bmi && bmi >= 30) || (age && age >= 55))) {
    why.push('Para poupar as articulações, deixei de fora exercícios com saltos e impacto.');
  }
  if (focus.size) why.push('Mais volume para: ' + p.foco.map(f => ASST_OPTS.foco.find(o => o[0] === f)[1].toLowerCase()).join(', ') + '.');
  why.push(daysPlan
    ? `Dias: ${sessions.map(s => `${s.name.split(' — ')[0]} ${daysLabel(s.days)}`).join(' · ')}.`
    : `Treine na ordem (${sessions.map((_, i) => letter(i)).join(' → ')}) nos ${p.dias} dias que preferir — o app sugere a próxima ficha.`);
  return {
    name: splitName, sub: `${p.dias}x por semana · cerca de ${Math.round(maxMin / 5) * 5} min`,
    why, sessions, programName: `Meu treino — ${splitName}`
  };
}
function asstNote(p, c, noted) {
  for (const r of p.restr) {
    for (const [slots, txt] of ASST_RESTR_NOTE[r] || []) {
      if (!noted.has(txt) && slots.split(' ').includes(c.slot)) { noted.add(txt); return txt; }
    }
  }
  return ASST_EX_NOTE[c.id] || '';
}
function asstGenerateLocal(p, seed = 0) {
  return asstSplitKeys(p).map(k => asstBuildOption(p, k, seed));
}

/* ---------- IA (Claude) ---------- */
const ASST_AI_KEY = 'ficha.ai.key'; // fica só neste aparelho; não vai para o backup
const ASST_AI_MODEL = 'claude-opus-5-5';
function aiKey() { try { return localStorage.getItem(ASST_AI_KEY) || ''; } catch (e) { return ''; } }
function aiSetKey(k) { try { k ? localStorage.setItem(ASST_AI_KEY, k) : localStorage.removeItem(ASST_AI_KEY); } catch (e) { /* ignora */ } }
let aiSdkPromise = null;
function aiSdk() {
  // SDK oficial da Anthropic, empacotado em vendor/ e carregado só quando a IA é usada
  if (!aiSdkPromise) aiSdkPromise = import('./vendor/anthropic-sdk.mjs').then(m => m.default).catch(e => { aiSdkPromise = null; throw e; });
  return aiSdkPromise;
}
const AI_SCHEMA = {
  type: 'object', additionalProperties: false, required: ['observacoes', 'opcoes'],
  properties: {
    observacoes: { type: 'string' },
    opcoes: {
      type: 'array', items: {
        type: 'object', additionalProperties: false, required: ['nome', 'resumo', 'porque', 'minutos', 'fichas'],
        properties: {
          nome: { type: 'string' }, resumo: { type: 'string' }, minutos: { type: 'integer' },
          porque: { type: 'array', items: { type: 'string' } },
          fichas: {
            type: 'array', items: {
              type: 'object', additionalProperties: false, required: ['nome', 'dias', 'exercicios'],
              properties: {
                nome: { type: 'string' },
                dias: { type: 'array', items: { type: 'integer' } },
                exercicios: {
                  type: 'array', items: {
                    type: 'object', additionalProperties: false, required: ['id', 'series', 'reps', 'descanso', 'obs'],
                    properties: { id: { type: 'string' }, series: { type: 'integer' }, reps: { type: 'string' }, descanso: { type: 'integer' }, obs: { type: 'string' } }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
};
function aiCatalog(p) {
  const allowed = { academia: null, casa_halteres: ['Halteres', 'Peso corporal'], casa: ['Peso corporal'] }[p.local];
  return BUILTIN_EXERCISES.concat(S.custom)
    .filter(e => !allowed || allowed.includes(e.equip))
    .map(e => `${e.id} | ${e.name} | ${e.group} | ${e.equip} | ${e.kind}`).join('\n');
}
function aiSystem(p) {
  return `Você é um treinador experiente, formado em educação física, montando programas de treino para um app de academia. Escreva em português do Brasil, de forma clara e curta.

Monte exatamente 3 opções de programa, diferentes entre si (por exemplo, divisões de treino diferentes), ordenadas da mais recomendada para a menos recomendada para esta pessoa.

Regras:
- Use somente exercícios do catálogo abaixo, sempre pelo id exato.
- Respeite todas as restrições e observações da pessoa. Na dúvida, escolha a alternativa mais segura e explique no campo "obs" do exercício.
- O número de fichas e os dias devem combinar com os dias por semana informados. "dias" usa 0 = domingo, 1 = segunda … 6 = sábado. Se as fichas forem feitas em sequência, sem dia fixo, deixe "dias" vazio.
- Cada ficha precisa caber no tempo por treino informado, contando séries, descansos e cardio. Informe em "minutos" a duração aproximada da ficha mais longa.
- "reps" é texto: faixa de repetições ("8-12"); para exercícios do tipo s, segundos ("30-45"); para cardio (tipo c), minutos ("20") com "series" = 1.
- "descanso" em segundos, um destes valores: 0, 30, 45, 60, 75, 90, 120, 150, 180, 240, 300.
- "obs": dica curta de execução ou ajuste para esta pessoa (pode ficar vazio).
- "resumo": uma frase descrevendo a opção. "porque": 3 a 5 frases curtas dizendo por que ela serve para esta pessoa.
- "observacoes": recomendações gerais curtas (aquecimento, progressão de carga, cuidados).
- Não faça diagnóstico médico. Se houver dor, lesão ou condição de saúde, recomende acompanhamento de um profissional.

Catálogo (id | nome | grupo | equipamento | tipo — w = carga × repetições, bw = peso do corpo, s = segundos, c = cardio):
${aiCatalog(p)}`;
}
function aiUser(p) {
  const o = (k, v) => (ASST_OPTS[k].find(x => x[0] === v) || [, v])[1];
  const bmi = asstBmi(p);
  const lines = [
    `Objetivo: ${o('objetivo', p.objetivo)}`,
    `Experiência: ${o('nivel', p.nivel)} (${ASST_LEVEL_HINT[p.nivel]})`,
    `Dias por semana: ${p.dias}`,
    `Tempo por treino: ${p.tempo} minutos`,
    `Local: ${o('local', p.local)}`,
    p.sexo ? `Sexo: ${o('sexo', p.sexo)}` : '',
    num(p.idade) ? `Idade: ${num(p.idade)} anos` : '',
    num(p.peso) ? `Peso: ${num(p.peso)} kg` : '',
    num(p.altura) ? `Altura: ${num(p.altura)} cm` : '',
    bmi ? `IMC: ${bmi.toFixed(1)}` : '',
    p.foco.length ? `Quer dar prioridade a: ${p.foco.map(f => o('foco', f)).join(', ')}` : '',
    p.restr.length ? `Restrições: ${p.restr.map(r => o('restr', r)).join(', ')}` : 'Restrições: nenhuma informada',
    p.obs.trim() ? `Observações da pessoa: ${p.obs.trim()}` : ''
  ].filter(Boolean);
  return `Monte as 3 opções de programa para esta pessoa:\n${lines.map(l => '- ' + l).join('\n')}`;
}
function aiToOptions(data) {
  let dropped = 0;
  const snap = v => REST_OPTIONS.reduce((a, b) => Math.abs(b - v) < Math.abs(a - v) ? b : a, 0);
  const opts = (data.opcoes || []).map(o => {
    const sessions = (o.fichas || []).map(f => ({
      name: String(f.nome || 'Ficha').slice(0, 60),
      days: [...new Set((f.dias || []).filter(d => Number.isInteger(d) && d >= 0 && d <= 6))].sort(),
      items: (f.exercicios || []).filter(x => { const ok = !!getEx(x.id); if (!ok) dropped++; return ok; })
        .map(x => [x.id, Math.max(1, Math.min(10, x.series | 0 || 3)), String(x.reps || '').slice(0, 12), snap(+x.descanso || 0), String(x.obs || '').slice(0, 140)])
    })).filter(s => s.items.length);
    return {
      name: String(o.nome || 'Opção'), sub: `${sessions.length} ficha${sessions.length > 1 ? 's' : ''}${o.minutos ? ` · cerca de ${o.minutos} min` : ''}`,
      why: [o.resumo, ...(o.porque || [])].filter(Boolean).map(String), sessions, programName: String(o.nome || 'Meu treino').slice(0, 60)
    };
  }).filter(o => o.sessions.length);
  return { opts, notes: String(data.observacoes || ''), dropped };
}
class AiError extends Error {}
async function aiGenerate(p, onProgress, holder) {
  const Anthropic = await aiSdk();
  const client = new Anthropic({ apiKey: aiKey(), dangerouslyAllowBrowser: true, maxRetries: 1 });
  try {
    const stream = client.beta.messages.stream({
      model: ASST_AI_MODEL,
      max_tokens: 32000,
      // Se o modelo recusar o pedido, a API tenta de novo num modelo alternativo recomendado
      betas: ['server-side-fallback-2026-07-01'],
      fallbacks: 'default',
      output_config: { effort: 'medium', format: { type: 'json_schema', schema: AI_SCHEMA } },
      cache_control: { type: 'ephemeral' }, // o catálogo (system) fica em cache entre gerações
      system: aiSystem(p),
      messages: [{ role: 'user', content: aiUser(p) }]
    });
    holder.stream = stream;
    let chars = 0;
    stream.on('streamEvent', ev => {
      if (ev.type === 'content_block_start' && ev.content_block.type === 'thinking') onProgress('pensando', 0);
    });
    stream.on('text', delta => { chars += delta.length; onProgress('escrevendo', chars); });
    const msg = await stream.finalMessage();
    if (msg.stop_reason === 'refusal') throw new AiError('A IA não conseguiu atender este pedido. Tente mudar as observações.');
    if (msg.stop_reason === 'max_tokens') throw new AiError('A resposta ficou incompleta. Tente de novo.');
    const text = msg.content.filter(b => b.type === 'text').map(b => b.text).join('');
    let data;
    try { data = JSON.parse(text); } catch (e) { throw new AiError('A resposta da IA veio num formato inesperado. Tente de novo.'); }
    const res = aiToOptions(data);
    if (!res.opts.length) throw new AiError('A IA não montou nenhuma opção válida. Tente de novo.');
    return res;
  } catch (e) {
    if (e instanceof AiError) throw e;
    if (e instanceof Anthropic.APIUserAbortError) throw e;
    if (e instanceof Anthropic.AuthenticationError) throw new AiError('Chave da API inválida. Confira a chave em Ajustes → Inteligência artificial.');
    if (e instanceof Anthropic.PermissionDeniedError) throw new AiError('Esta chave não tem permissão para usar o modelo. Confira sua conta na Anthropic.');
    if (e instanceof Anthropic.RateLimitError) throw new AiError('Limite de uso atingido. Espere um pouco e tente de novo.');
    if (e instanceof Anthropic.BadRequestError) throw new AiError('A Anthropic recusou a solicitação: ' + (e.error?.error?.message || e.message));
    if (e instanceof Anthropic.InternalServerError) throw new AiError('O serviço da IA está ocupado agora. Tente de novo em instantes.');
    if (e instanceof Anthropic.APIConnectionError) throw new AiError('Sem conexão com a internet (a IA precisa de internet).');
    if (e instanceof Anthropic.APIError) throw new AiError(`Erro da IA (${e.status || '?'}): ${e.message}`);
    throw e;
  }
}

/* ---------- Telas ---------- */
let asst = { step: 'form', opts: [], source: 'local', notes: '', seed: 0, dropped: 0 };

function asstChips(field, multi) {
  const p = asstProfile(), cur = p[field];
  return `<div class="chips wrap">${ASST_OPTS[field].map(([v, l]) => {
    const on = multi ? cur.includes(v) : String(cur) === String(v);
    return `<button class="chip ${on ? 'on' : ''}" data-act="asstSet" data-f="${field}" data-v="${v}" ${multi ? 'data-multi="1"' : ''} aria-pressed="${on}">${l}</button>`;
  }).join('')}</div>`;
}
function viewAssistente() {
  return asst.step === 'results' && asst.opts.length ? asstResults() : asstForm();
}
function asstForm() {
  const p = asstProfile(), bmi = asstBmi(p);
  const field = (f, label, ph, mode = 'decimal') => `<label><span>${label}</span><input class="input num" type="text" inputmode="${mode}" value="${esc(p[f])}" placeholder="${ph}" data-pf="${f}"></label>`;
  return topBar({ back: '#/fichas' }) + `<div class="eyebrow">Assistente de treino</div><h1 class="title">Montar meu treino</h1>
    <p class="muted" style="margin:-8px 2px 16px;line-height:1.45">Responda e receba 3 opções de programa para escolher. Depois dá para mudar tudo.</p>
    <div class="card asst">
      <div class="look-label">Objetivo</div>${asstChips('objetivo')}
      <div class="look-label">Experiência com treino</div>${asstChips('nivel')}
      <div class="asst-row">
        <div><div class="look-label">Dias por semana</div>${asstChips('dias')}</div>
      </div>
      <div class="look-label">Tempo por treino (minutos)</div>${asstChips('tempo')}
      <div class="look-label">Onde vai treinar</div>${asstChips('local')}
    </div>
    <div class="card asst">
      <div class="look-label">Sobre você <span class="muted" style="font-weight:600">(opcional)</span></div>
      <div class="mini-grid">${field('peso', 'Peso (kg)', '70')}${field('altura', 'Altura (cm)', '170')}${field('idade', 'Idade', '30', 'numeric')}</div>
      ${bmi ? `<p class="small muted" style="margin:8px 2px 0">IMC ${fmt(bmi)} · usado só para escolher exercícios mais seguros</p>` : ''}
      <div class="look-label" style="margin-top:14px">Sexo</div>${asstChips('sexo')}
    </div>
    <div class="card asst">
      <div class="look-label">Quer dar prioridade a algum grupo?</div>${asstChips('foco', true)}
      <div class="look-label">Restrições ou dores</div>${asstChips('restr', true)}
      <label class="field" style="margin:4px 0 0"><span>Outras observações (usadas pela IA)</span>
        <textarea class="input" data-pf="obs" placeholder="Ex.: hérnia de disco, já treino há 1 ano, prefiro máquinas, não gosto de agachamento…">${esc(p.obs)}</textarea></label>
    </div>
    <div class="stack" style="margin-top:6px">
      <button class="btn primary block" data-act="asstLocal">${I.list}Ver opções</button>
      <button class="btn block" data-act="asstAi">${I.sparkle}Montar com IA (Claude)</button>
    </div>
    <p class="small muted" style="margin:12px 4px 0;line-height:1.5">“Ver opções” usa regras de treino, funciona sem internet e é grátis. “Montar com IA” usa o Claude, entende as suas observações em texto livre e precisa de internet e de uma chave da API da Anthropic (paga por uso).</p>
    ${asstDisclaimer()}`;
}
function asstDisclaimer() {
  return `<p class="small muted asst-disc">Sugestão automática: não substitui a avaliação de um profissional de educação física. Com dor, lesão ou condição de saúde, procure orientação médica antes de treinar.</p>`;
}
function asstResults() {
  const p = asstProfile(), bmi = asstBmi(p);
  const o = (k, v) => (ASST_OPTS[k].find(x => String(x[0]) === String(v)) || [, v])[1];
  const summary = [o('objetivo', p.objetivo), o('nivel', p.nivel), `${p.dias}x por semana`, `${p.tempo} min`, o('local', p.local), bmi ? `IMC ${fmt(bmi)}` : ''].filter(Boolean).join(' · ');
  let html = topBar({ back: '#/fichas', right: `<button class="link-btn" data-act="asstEdit">Ajustar respostas</button>` }) +
    `<div class="eyebrow">${asst.source === 'ai' ? 'Montado com IA (Claude)' : 'Assistente de treino'}</div><h1 class="title">Suas opções</h1>
    <p class="small muted" style="margin:-8px 2px 14px">${esc(summary)}</p>`;
  if (asst.source === 'ai' && asst.notes) html += `<div class="card asst-notes"><b>Recomendações</b><p>${esc(asst.notes)}</p></div>`;
  if (asst.dropped) html += `<p class="small muted" style="margin:0 4px 10px">${asst.dropped} exercício(s) sugerido(s) pela IA não estavam no catálogo e foram removidos.</p>`;
  html += asst.opts.map((op, i) => `<div class="card asst-opt ${i === 0 ? 'best' : ''}">
      <div class="asst-opt-top"><span class="badge ${i === 0 ? 'accent' : ''}">${i === 0 ? 'Recomendada' : `Opção ${i + 1}`}</span><span class="small muted">${esc(op.sub)}</span></div>
      <div class="hero-title" style="margin:8px 0 6px">${esc(op.name)}</div>
      <ul class="asst-why">${op.why.map(w => `<li>${esc(w)}</li>`).join('')}</ul>
      <details ${i === 0 ? 'open' : ''}><summary>Ver as ${op.sessions.length} fichas</summary>
        ${op.sessions.map((s, k) => `<div class="asst-ficha"><div class="asst-ficha-head"><b>${letter(k)} · ${esc(s.name)}</b><span class="small muted">${s.days.length ? daysLabel(s.days) : 'Sem dia fixo'}</span></div>
          ${s.items.map(([id, sets, reps, rest, note]) => {
            const ex = getEx(id), k2 = exKind(id);
            return `<button class="asst-ex" data-act="howTo" data-id="${id}">${thumb(ex)}<span class="grow"><span class="name">${esc(exName(id))}</span>
              <span class="sub num">${sets} × ${esc(reps)}${k2 === 's' ? ' s' : k2 === 'c' ? ' min' : ''}${rest ? ` · ${fmtRest(rest)}` : ''}${note ? ` · ${esc(note)}` : ''}</span></span></button>`;
          }).join('')}</div>`).join('')}
      </details>
      <button class="btn primary block" style="margin-top:12px" data-act="asstUse" data-i="${i}">${I.check}Usar esta opção</button>
    </div>`).join('');
  html += `<div class="stack" style="margin-top:6px">
      ${asst.source === 'local' ? `<button class="btn block" data-act="asstReroll">${I.copy}Outra variação de exercícios</button>` : `<button class="btn block" data-act="asstAi">${I.sparkle}Gerar de novo com IA</button>`}
      ${asst.source === 'local' ? `<button class="btn block" data-act="asstAi">${I.sparkle}Montar com IA (Claude)</button>` : `<button class="btn block" data-act="asstLocal">${I.list}Ver opções sem IA</button>`}
    </div>${asstDisclaimer()}`;
  return html;
}

function aiKeySheet(then) {
  const has = !!aiKey();
  openSheet(`${sheetHead('Chave da API da Anthropic')}<div class="sheet-body">
      <p class="muted" style="margin:0 0 10px;line-height:1.5">Para usar a IA, crie uma chave em <a href="https://platform.claude.com" target="_blank" rel="noopener" style="color:var(--accent-text);text-decoration:underline">platform.claude.com</a> (Settings → API keys) e cole aqui. O uso é cobrado pela Anthropic na sua conta — cada geração custa em torno de US$ 0,15 a 0,40.</p>
      <label class="field"><span>Chave (começa com sk-ant-)</span>
        <input class="input" id="aiKeyInput" type="password" autocomplete="off" autocapitalize="off" spellcheck="false" placeholder="${has ? 'Chave salva — cole outra para trocar' : 'sk-ant-…'}"></label>
      <p class="small muted" style="margin:0 2px">A chave fica salva só neste aparelho e não vai para o backup. As suas respostas do assistente são enviadas à Anthropic para gerar o treino.</p>
    </div>
    <div class="sheet-foot stack">
      <button class="btn primary block" data-act="aiKeySave" data-then="${then || ''}">Salvar${then ? ' e montar treino' : ''}</button>
      ${has ? '<button class="btn danger block" data-act="aiKeyRemove">Remover chave</button>' : ''}
    </div>`);
  setTimeout(() => { const el = $('#aiKeyInput'); if (el) el.focus(); }, 300);
}

let aiRun = null;
function aiStart() {
  if (!aiKey()) { aiKeySheet('gen'); return; }
  if (!navigator.onLine) { toast('Sem internet — a IA precisa de conexão'); return; }
  const p = asstProfile(), holder = {}, t0 = Date.now();
  let phase = 'Preparando…';
  aiRun = holder;
  openSheet(`<div class="sheet-body"><div class="ai-run">
      <div class="ai-orb">${I.sparkle}</div>
      <h3>Montando seu treino com IA</h3>
      <p class="muted" data-ai-phase>${phase}</p>
      <p class="small muted num" data-ai-time>0 s</p>
    </div></div>
    <div class="sheet-foot"><button class="btn block" data-act="aiCancel">Cancelar</button></div>`, { aiRun: true });
  const timer = setInterval(() => { const el = $('[data-ai-time]'); if (el) el.textContent = `${Math.round((Date.now() - t0) / 1000)} s`; }, 1000);
  const setPhase = (ph, chars) => {
    phase = ph === 'pensando' ? 'Analisando seu perfil e escolhendo a divisão…' : `Escrevendo as fichas… (${chars} caracteres)`;
    const el = $('[data-ai-phase]'); if (el) el.textContent = phase;
  };
  aiGenerate(p, setPhase, holder).then(res => {
    clearInterval(timer);
    if (aiRun !== holder) return;
    aiRun = null;
    asst = { step: 'results', opts: res.opts, source: 'ai', notes: res.notes, seed: 0, dropped: res.dropped };
    closeSheet();
    if (location.hash === '#/assistente') { rerender(); window.scrollTo(0, 0); } else go('#/assistente');
  }).catch(e => {
    clearInterval(timer);
    if (aiRun !== holder) return;
    aiRun = null;
    if (e && e.name === 'APIUserAbortError') { closeSheet(); return; }
    console.warn(e);
    openSheet(`${sheetHead('Não deu certo')}<div class="sheet-body"><p class="muted" style="margin:0 0 6px;line-height:1.5">${esc(e && e.message || 'Erro desconhecido')}</p></div>
      <div class="sheet-foot stack"><button class="btn primary block" data-act="asstAi">Tentar de novo</button>
      <button class="btn block" data-act="asstLocal">Ver opções sem IA</button></div>`);
  });
}

// Ações do assistente (incorporadas ao ACT do app.js)
const ASSIST_ACTIONS = {
  asstSet: el => {
    const f = el.dataset.f, raw = el.dataset.v, p = asstProfile();
    const v = ['dias', 'tempo'].includes(f) ? +raw : raw;
    if (el.dataset.multi) p[f] = p[f].includes(v) ? p[f].filter(x => x !== v) : [...p[f], v];
    else p[f] = v;
    S.profile = p; save(); rerender();
  },
  asstLocal: () => {
    closeSheet();
    asst = { step: 'results', opts: asstGenerateLocal(asstProfile(), 0), source: 'local', notes: '', seed: 0, dropped: 0 };
    if (location.hash === '#/assistente') { rerender(); window.scrollTo(0, 0); } else go('#/assistente');
  },
  asstReroll: () => {
    asst.seed++;
    asst.opts = asstGenerateLocal(asstProfile(), asst.seed);
    rerender(); toast('Nova variação de exercícios');
  },
  asstEdit: () => { asst.step = 'form'; rerender(); window.scrollTo(0, 0); },
  asstAi: () => { closeSheet(); aiStart(); },
  aiCancel: () => { const h = aiRun; aiRun = null; if (h && h.stream) h.stream.abort(); closeSheet(); },
  aiKeySave: el => {
    const v = ($('#aiKeyInput').value || '').trim();
    if (!v) { toast('Cole a chave da API'); return; }
    if (!/^sk-ant-/.test(v)) { toast('A chave deve começar com sk-ant-'); return; }
    aiSetKey(v); closeSheet(); toast('Chave salva neste aparelho');
    if (el.dataset.then === 'gen') aiStart(); else rerender();
  },
  aiKeyRemove: () => { aiSetKey(''); closeSheet(); toast('Chave removida'); rerender(); },
  aiKeyEdit: () => aiKeySheet(''),
  asstUse: el => {
    const op = asst.opts[+el.dataset.i];
    openUseProgram({ name: op.programName, routines: op.sessions.map(s => ({ name: s.name, days: s.days, items: s.items })) });
  }
};
function onProfileInput(t) {
  const p = asstProfile();
  p[t.dataset.pf] = t.dataset.pf === 'obs' ? t.value.slice(0, 600) : t.value.replace(/[^\d.,]/g, '');
  S.profile = p; save();
}
