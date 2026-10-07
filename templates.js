// Modelos de programas de treino prontos.
// Cada ficha: { name, days, items } — days: 0 = Dom … 6 = Sáb (dias sugeridos)
// Cada exercício: [id do exercício, séries, repetições (ou segundos / minutos), descanso em segundos]
'use strict';

const TEMPLATES = [
  {
    id: 'abc', name: 'ABC clássico', level: 'Intermediário', freq: '3 ou 6x por semana', place: 'academia',
    desc: 'O mais comum nas academias: peito/ombro/tríceps, costas/bíceps e pernas. Faça A, B e C em sequência, 3 ou 6 dias por semana.',
    routines: [
      { name: 'A — Peito, ombro e tríceps', days: [1, 4], items: [
        ['supino-reto-barra', 4, '8-10', 120], ['supino-inclinado-halter', 3, '10-12', 90], ['crossover-alto', 3, '12-15', 60],
        ['desenvolvimento-halter', 3, '8-12', 90], ['elevacao-lateral', 3, '12-15', 60], ['triceps-corda', 3, '10-12', 60],
        ['triceps-frances', 3, '10-12', 60]
      ] },
      { name: 'B — Costas e bíceps', days: [2, 5], items: [
        ['puxada-aberta', 4, '8-12', 90], ['remada-curvada', 4, '8-10', 120], ['serrote', 3, '10-12', 90],
        ['face-pull', 3, '15', 60], ['rosca-direta', 3, '8-12', 60], ['rosca-martelo', 3, '10-12', 60]
      ] },
      { name: 'C — Pernas e abdômen', days: [3, 6], items: [
        ['agachamento', 4, '6-10', 150], ['leg-press-45', 3, '10-12', 120], ['extensora', 3, '12-15', 60],
        ['mesa-flexora', 3, '10-12', 60], ['stiff-barra', 3, '8-10', 120], ['panturrilha-em-pe', 4, '12-15', 60],
        ['prancha', 3, '45', 45]
      ] }
    ]
  },
  {
    id: 'ppl', name: 'Push / Pull / Legs', level: 'Intermediário', freq: '3 ou 6x por semana', place: 'academia',
    desc: 'Divide o treino por movimento: empurrar (peito, ombro, tríceps), puxar (costas, bíceps) e pernas. Muito eficiente para hipertrofia; faça o ciclo 2x na semana se tiver tempo.',
    routines: [
      { name: 'Push — peito, ombro e tríceps', days: [1, 4], items: [
        ['supino-reto-barra', 4, '6-10', 150], ['supino-inclinado-halter', 3, '8-12', 90], ['desenvolvimento-halter', 3, '8-12', 90],
        ['crossover-alto', 3, '12-15', 60], ['elevacao-lateral', 4, '12-20', 60], ['triceps-pulley', 3, '10-12', 60],
        ['triceps-frances', 3, '10-12', 60]
      ] },
      { name: 'Pull — costas e bíceps', days: [2, 5], items: [
        ['barra-fixa', 4, '6-10', 120], ['remada-curvada', 4, '8-10', 120], ['puxada-triangulo', 3, '10-12', 90],
        ['remada-baixa', 3, '10-12', 90], ['face-pull', 3, '15-20', 60], ['rosca-direta', 3, '8-12', 60],
        ['rosca-martelo', 3, '10-12', 60]
      ] },
      { name: 'Legs — pernas', days: [3, 6], items: [
        ['agachamento', 4, '6-10', 180], ['terra-romeno', 3, '8-10', 150], ['leg-press-45', 3, '10-15', 120],
        ['cadeira-flexora', 3, '10-15', 60], ['extensora', 3, '12-15', 60], ['panturrilha-em-pe', 4, '10-15', 60],
        ['abdominal-polia', 3, '12-15', 60]
      ] }
    ]
  },
  {
    id: 'upper-lower', name: 'Upper / Lower', level: 'Intermediário', freq: '4x por semana', place: 'academia',
    desc: 'Superiores e inferiores alternados: cada músculo treina 2x por semana. Ótimo equilíbrio entre força e volume.',
    routines: [
      { name: 'Upper A — superiores', days: [1], items: [
        ['supino-reto-barra', 4, '6-8', 150], ['remada-curvada', 4, '6-8', 150], ['desenvolvimento-halter', 3, '8-10', 90],
        ['puxada-aberta', 3, '8-12', 90], ['rosca-direta', 2, '10-12', 60], ['triceps-pulley', 2, '10-12', 60]
      ] },
      { name: 'Lower A — inferiores', days: [2], items: [
        ['agachamento', 4, '6-8', 180], ['terra-romeno', 3, '8-10', 150], ['leg-press-45', 3, '10-12', 120],
        ['mesa-flexora', 3, '10-12', 60], ['panturrilha-em-pe', 4, '10-15', 60], ['prancha', 3, '45', 45]
      ] },
      { name: 'Upper B — superiores', days: [4], items: [
        ['supino-inclinado-halter', 4, '8-10', 90], ['puxada-supinada', 4, '8-10', 90], ['serrote', 3, '10-12', 90],
        ['elevacao-lateral', 4, '12-15', 60], ['peck-deck', 3, '12-15', 60], ['rosca-martelo', 3, '10-12', 60],
        ['triceps-corda', 3, '10-12', 60]
      ] },
      { name: 'Lower B — inferiores', days: [5], items: [
        ['terra', 3, '5', 180], ['bulgaro', 3, '8-10', 90], ['hip-thrust', 3, '8-12', 90],
        ['extensora', 3, '12-15', 60], ['cadeira-flexora', 3, '12-15', 60], ['panturrilha-sentado', 4, '12-15', 60],
        ['elevacao-pernas', 3, '10-15', 60]
      ] }
    ]
  },
  {
    id: 'iniciante', name: 'Iniciante — corpo inteiro', level: 'Iniciante', freq: '3x por semana', place: 'academia',
    desc: 'Corpo inteiro em todo treino, com máquinas e exercícios simples. Ideal para os primeiros meses: aprende os movimentos e treina cada músculo 3x por semana.',
    routines: [
      { name: 'Corpo inteiro A', days: [1], items: [
        ['leg-press-45', 3, '10-12', 90], ['supino-maquina', 3, '10-12', 90], ['puxada-aberta', 3, '10-12', 90],
        ['desenvolvimento-maquina', 3, '10-12', 90], ['mesa-flexora', 3, '10-12', 60], ['abdominal-supra', 3, '15', 45]
      ] },
      { name: 'Corpo inteiro B', days: [3], items: [
        ['goblet', 3, '10-12', 90], ['supino-reto-halter', 3, '10-12', 90], ['remada-baixa', 3, '10-12', 90],
        ['elevacao-lateral', 3, '12-15', 60], ['extensora', 3, '12-15', 60], ['prancha', 3, '30', 45]
      ] },
      { name: 'Corpo inteiro C', days: [5], items: [
        ['hack', 3, '10-12', 90], ['peck-deck', 3, '12-15', 60], ['remada-maquina', 3, '10-12', 90],
        ['rosca-alternada', 3, '10-12', 60], ['triceps-pulley', 3, '10-12', 60], ['cadeira-flexora', 3, '12-15', 60],
        ['panturrilha-sentado', 3, '15', 45]
      ] }
    ]
  },
  {
    id: 'abcd', name: 'ABCD', level: 'Intermediário', freq: '4x por semana', place: 'academia',
    desc: 'Quatro fichas: peito e tríceps, costas e bíceps, pernas, ombros e abdômen. Mais volume por grupo que o ABC.',
    routines: [
      { name: 'A — Peito e tríceps', days: [1], items: [
        ['supino-reto-barra', 4, '8-10', 120], ['supino-inclinado-halter', 3, '10-12', 90], ['peck-deck', 3, '12-15', 60],
        ['crossover-baixo', 3, '12-15', 60], ['triceps-pulley', 3, '10-12', 60], ['triceps-testa', 3, '10-12', 60]
      ] },
      { name: 'B — Costas e bíceps', days: [2], items: [
        ['puxada-aberta', 4, '8-12', 90], ['remada-curvada', 3, '8-10', 120], ['remada-baixa', 3, '10-12', 90],
        ['pulldown', 3, '12-15', 60], ['rosca-direta', 3, '8-12', 60], ['rosca-scott', 3, '10-12', 60]
      ] },
      { name: 'C — Pernas', days: [4], items: [
        ['agachamento', 4, '8-10', 150], ['leg-press-45', 4, '10-12', 120], ['extensora', 3, '12-15', 60],
        ['mesa-flexora', 4, '10-12', 60], ['stiff-barra', 3, '10-12', 90], ['panturrilha-em-pe', 4, '12-15', 60]
      ] },
      { name: 'D — Ombros e abdômen', days: [5], items: [
        ['desenvolvimento-barra', 4, '8-10', 120], ['elevacao-lateral', 4, '12-15', 60], ['elevacao-frontal', 3, '12', 60],
        ['crucifixo-invertido', 3, '12-15', 60], ['encolhimento-halter', 3, '12-15', 60], ['abdominal-infra', 3, '15', 45],
        ['prancha', 3, '45', 45]
      ] }
    ]
  },
  {
    id: 'abcde', name: 'ABCDE — um grupo por dia', level: 'Avançado', freq: '5x por semana', place: 'academia',
    desc: 'O clássico “bro split”: um grupo muscular por dia, com bastante volume. Para quem já treina há um bom tempo e consegue ir 5 dias.',
    routines: [
      { name: 'A — Peito', days: [1], items: [
        ['supino-reto-barra', 4, '6-10', 150], ['supino-inclinado-barra', 4, '8-10', 120], ['crucifixo-inclinado', 3, '10-12', 90],
        ['peck-deck', 3, '12-15', 60], ['crossover-alto', 3, '12-15', 60], ['mergulho-peito', 3, '8-12', 90]
      ] },
      { name: 'B — Costas', days: [2], items: [
        ['terra', 4, '5', 180], ['barra-fixa', 4, '6-10', 120], ['remada-curvada', 4, '8-10', 120],
        ['serrote', 3, '10-12', 90], ['puxada-triangulo', 3, '10-12', 90], ['pulldown', 3, '12-15', 60]
      ] },
      { name: 'C — Pernas', days: [3], items: [
        ['agachamento', 5, '6-10', 180], ['hack', 4, '8-12', 120], ['leg-press-45', 3, '12-15', 120],
        ['extensora', 3, '12-15', 60], ['mesa-flexora', 4, '10-12', 60], ['terra-romeno', 3, '8-10', 120],
        ['panturrilha-em-pe', 5, '10-15', 60]
      ] },
      { name: 'D — Ombros e trapézio', days: [4], items: [
        ['desenvolvimento-halter', 4, '8-10', 120], ['arnold', 3, '10-12', 90], ['elevacao-lateral', 4, '12-15', 60],
        ['elevacao-lateral-polia', 3, '12-15', 60], ['voador-inverso', 3, '12-15', 60], ['face-pull', 3, '15', 60],
        ['encolhimento-barra', 4, '10-12', 60]
      ] },
      { name: 'E — Braços e abdômen', days: [5], items: [
        ['rosca-direta', 4, '8-10', 60], ['triceps-testa', 4, '8-10', 60], ['rosca-inclinada', 3, '10-12', 60],
        ['triceps-corda', 3, '10-12', 60], ['rosca-martelo', 3, '10-12', 60], ['triceps-frances', 3, '10-12', 60],
        ['abdominal-polia', 3, '12-15', 45], ['elevacao-pernas', 3, '10-15', 45]
      ] }
    ]
  },
  {
    id: 'phul', name: 'PHUL — força e hipertrofia', level: 'Avançado', freq: '4x por semana', place: 'academia',
    desc: 'Power Hypertrophy Upper Lower: dois dias pesados de força (poucas repetições) e dois dias de hipertrofia, alternando superiores e inferiores.',
    routines: [
      { name: 'Upper força', days: [1], items: [
        ['supino-reto-barra', 4, '3-5', 180], ['remada-curvada', 4, '3-5', 180], ['desenvolvimento-barra', 3, '5-8', 150],
        ['puxada-aberta', 3, '6-10', 120], ['rosca-direta', 3, '6-10', 90], ['triceps-testa', 3, '6-10', 90]
      ] },
      { name: 'Lower força', days: [2], items: [
        ['agachamento', 4, '3-5', 180], ['terra', 3, '3-5', 180], ['leg-press-45', 4, '10-15', 120],
        ['mesa-flexora', 4, '6-10', 90], ['panturrilha-em-pe', 4, '6-10', 60]
      ] },
      { name: 'Upper hipertrofia', days: [4], items: [
        ['supino-inclinado-halter', 4, '8-12', 90], ['crucifixo-reto', 3, '8-12', 60], ['remada-baixa', 4, '8-12', 90],
        ['serrote', 3, '8-12', 90], ['elevacao-lateral', 4, '10-12', 60], ['rosca-inclinada', 4, '8-12', 60],
        ['triceps-corda', 4, '8-12', 60]
      ] },
      { name: 'Lower hipertrofia', days: [5], items: [
        ['agachamento-frontal', 4, '8-12', 150], ['bulgaro', 3, '8-12', 90], ['extensora', 4, '10-15', 60],
        ['cadeira-flexora', 4, '10-15', 60], ['hip-thrust', 3, '8-12', 90], ['panturrilha-sentado', 4, '8-12', 60]
      ] }
    ]
  },
  {
    id: 'arnold', name: 'Arnold split', level: 'Avançado', freq: '6x por semana', place: 'academia',
    desc: 'A divisão usada por Arnold Schwarzenegger: peito com costas, ombros com braços e pernas, cada ficha 2x por semana.',
    routines: [
      { name: 'Peito e costas', days: [1, 4], items: [
        ['supino-reto-barra', 4, '8-10', 120], ['barra-fixa', 4, '8-10', 120], ['supino-inclinado-halter', 3, '10-12', 90],
        ['remada-curvada', 3, '8-10', 120], ['crucifixo-reto', 3, '10-12', 60], ['pullover', 3, '12', 60]
      ] },
      { name: 'Ombros e braços', days: [2, 5], items: [
        ['desenvolvimento-barra', 4, '8-10', 120], ['elevacao-lateral', 4, '12-15', 60], ['crucifixo-invertido', 3, '12-15', 60],
        ['rosca-direta', 3, '8-10', 60], ['triceps-testa', 3, '8-10', 60], ['rosca-concentrada', 3, '10-12', 60],
        ['triceps-corda', 3, '10-12', 60]
      ] },
      { name: 'Pernas', days: [3, 6], items: [
        ['agachamento', 5, '6-10', 180], ['leg-press-45', 3, '10-12', 120], ['terra-romeno', 3, '8-10', 120],
        ['extensora', 3, '12-15', 60], ['mesa-flexora', 3, '10-12', 60], ['panturrilha-em-pe', 5, '10-15', 60],
        ['abdominal-polia', 3, '15', 45]
      ] }
    ]
  },
  {
    id: 'gluteos', name: 'Foco em glúteos e pernas', level: 'Intermediário', freq: '4x por semana', place: 'academia',
    desc: 'Dois treinos de inferiores (um voltado para glúteo e posterior, outro para quadríceps) e dois de superiores. Para quem prioriza pernas e glúteos.',
    routines: [
      { name: 'Inferior A — glúteo e posterior', days: [1], items: [
        ['hip-thrust', 4, '8-12', 120], ['terra-romeno', 3, '8-10', 120], ['bulgaro', 3, '10-12', 90],
        ['mesa-flexora', 3, '10-12', 60], ['abdutora', 3, '15-20', 60], ['gluteo-polia', 3, '12-15', 60]
      ] },
      { name: 'Superior A', days: [2], items: [
        ['supino-reto-halter', 3, '10-12', 90], ['puxada-aberta', 3, '10-12', 90], ['desenvolvimento-halter', 3, '10-12', 90],
        ['remada-baixa', 3, '10-12', 90], ['elevacao-lateral', 3, '12-15', 60], ['prancha', 3, '40', 45]
      ] },
      { name: 'Inferior B — quadríceps e glúteo', days: [4], items: [
        ['agachamento', 4, '8-10', 150], ['leg-press-45', 3, '10-12', 120], ['passada', 3, '10-12', 90],
        ['extensora', 3, '12-15', 60], ['ponte-de-gluteo-com-barra', 3, '12', 90], ['abdutora', 3, '15-20', 60],
        ['panturrilha-em-pe', 3, '15', 45]
      ] },
      { name: 'Superior B e abdômen', days: [5], items: [
        ['serrote', 3, '10-12', 90], ['supino-inclinado-halter', 3, '10-12', 90], ['face-pull', 3, '15', 60],
        ['rosca-alternada', 3, '10-12', 60], ['triceps-corda', 3, '10-12', 60], ['abdominal-infra', 3, '15', 45],
        ['abdominal-supra', 3, '15', 45]
      ] }
    ]
  },
  {
    id: 'halteres', name: 'Só halteres', level: 'Iniciante', freq: '3x por semana', place: 'casa',
    desc: 'Para treinar em casa ou numa academia pequena, só com um par de halteres e um banco. Alterne A e B.',
    routines: [
      { name: 'Corpo inteiro A', days: [1, 5], items: [
        ['agachamento-com-halteres', 3, '10-12', 90], ['supino-reto-halter', 3, '8-12', 90], ['serrote', 3, '10-12', 90],
        ['desenvolvimento-halter', 3, '10-12', 90], ['stiff-halter', 3, '10-12', 90], ['rosca-alternada', 2, '12', 60],
        ['triceps-frances', 2, '12', 60]
      ] },
      { name: 'Corpo inteiro B', days: [3], items: [
        ['goblet', 3, '12', 90], ['afundo-reverso-com-halteres', 3, '10', 90], ['supino-inclinado-halter', 3, '10-12', 90],
        ['remada-curvada-com-halteres', 3, '10-12', 90], ['elevacao-lateral', 3, '12-15', 60], ['rosca-martelo', 3, '12', 60],
        ['prancha', 3, '40', 45]
      ] }
    ]
  },
  {
    id: 'casa', name: 'Em casa — sem equipamento', level: 'Iniciante', freq: '3x por semana', place: 'casa',
    desc: 'Só com o peso do corpo, em qualquer lugar. Descanse pouco entre os exercícios. Quando ficar fácil, aumente as repetições ou use uma mochila com peso.',
    routines: [
      { name: 'Corpo inteiro A', days: [1], items: [
        ['agachamento-com-peso-corporal', 3, '15-20', 60], ['flexao', 3, '8-15', 60], ['passada-com-peso-corporal', 3, '10-12', 60],
        ['remada-invertida-remada-australiana', 3, '8-12', 60], ['ponte-gluteo', 3, '15-20', 45], ['prancha', 3, '30-45', 45]
      ] },
      { name: 'Corpo inteiro B', days: [3], items: [
        ['agachamento-com-salto', 3, '10-15', 60], ['flexao-de-braco-fechada-diamante', 3, '6-12', 60], ['ponte-de-gluteo-unilateral', 3, '10-12', 45],
        ['mergulho-banco', 3, '10-15', 60], ['escalador-mountain-climber', 3, '30', 45], ['abdominal-supra', 3, '15-20', 45],
        ['prancha-lateral', 3, '30', 30]
      ] },
      { name: 'Cardio e abdômen', days: [5], items: [
        ['burpee', 4, '10', 60], ['polichinelo', 4, '30', 30], ['escalador-mountain-climber', 3, '20', 45],
        ['agachamento-com-peso-corporal', 3, '20', 45], ['abdominal-bicicleta', 3, '20', 45], ['canoinha', 3, '20-30', 30],
        ['pular-corda', 1, '10', 0]
      ] }
    ]
  },
  {
    id: 'funcional', name: 'Funcional / condicionamento', level: 'Intermediário', freq: '3x por semana', place: 'academia',
    desc: 'Circuitos com kettlebell, medicine ball e peso do corpo para condicionamento e gasto calórico. Descansos curtos.',
    routines: [
      { name: 'Circuito A', days: [1], items: [
        ['kettlebell-swing', 4, '15', 45], ['burpee', 4, '10', 45], ['goblet', 4, '12', 45],
        ['remada-renegada-alternada', 3, '10', 45], ['slam-com-medicine-ball-acima-da-cabeca', 3, '12', 45], ['escalador-mountain-climber', 3, '30', 30]
      ] },
      { name: 'Circuito B', days: [3], items: [
        ['thruster-com-kettlebell', 4, '10', 60], ['salto-na-caixa-box-jump', 4, '8', 60], ['push-press', 3, '8', 90],
        ['caminhada-do-fazendeiro-farmer-s-walk', 3, '40 m', 60], ['corda-naval-battle-rope', 4, '30', 30], ['prancha-toque-ombro', 3, '20', 30]
      ] },
      { name: 'Circuito C', days: [5], items: [
        ['remo-ergometrico', 1, '10', 0], ['wall-ball', 4, '15', 45], ['afundo-com-salto-alternado', 3, '12', 45],
        ['flexao', 3, '12', 45], ['levantamento-turco-com-kettlebell-agachamento', 3, '5', 60], ['abdominal-remador', 3, '15', 30]
      ] }
    ]
  },
  {
    id: 'mobilidade', name: 'Alongamento e mobilidade', level: 'Todos os níveis', freq: 'Antes e depois do treino', place: 'casa',
    desc: 'Uma ficha de mobilidade para aquecer antes do treino e uma de alongamento para depois. Segure cada posição pelo tempo indicado (segundos).',
    routines: [
      { name: 'Mobilidade (aquecimento)', days: [], items: [
        ['circulos-com-os-bracos', 1, '30', 0], ['circulos-de-quadril-em-pe', 1, '30', 0], ['inchworm-caminhada-da-lagarta', 1, '40', 0],
        ['melhor-alongamento-do-mundo-world-s-greatest-stretch', 2, '30', 0], ['groiners-alongamento-dinamico-de-virilha', 1, '30', 0],
        ['alongamento-do-gato-cat-stretch', 1, '30', 0], ['balanco-frontal-de-perna-alongamento-dinamico', 1, '30', 0]
      ] },
      { name: 'Alongamento pós-treino', days: [], items: [
        ['alongamento-de-posterior-de-coxa', 2, '30', 0], ['alongamento-de-quadriceps-em-pe', 2, '30', 0],
        ['alongamento-de-gluteo-tornozelo-sobre-o-joelho', 2, '30', 0], ['alongamento-de-flexor-de-quadril-ajoelhado', 2, '30', 0],
        ['alongamento-de-peito-e-ombro-anterior', 2, '30', 0], ['alongamento-de-dorsal-acima-da-cabeca', 2, '30', 0],
        ['alongamento-de-triceps-acima-da-cabeca', 2, '30', 0], ['alongamento-de-panturrilha-com-maos-na-parede', 2, '30', 0],
        ['postura-da-crianca-alongamento', 1, '60', 0]
      ] }
    ]
  },
  {
    id: 'abdomen', name: 'Abdômen 15 minutos', level: 'Todos os níveis', freq: '2 a 4x por semana', place: 'casa',
    desc: 'Uma ficha rápida de abdômen para fazer no fim do treino ou em casa.',
    routines: [
      { name: 'Abdômen 15 min', days: [], items: [
        ['abdominal-supra', 3, '20', 30], ['abdominal-infra', 3, '15', 30], ['abdominal-bicicleta', 3, '20', 30],
        ['prancha', 3, '45', 30], ['prancha-lateral', 2, '30', 30], ['dead-bug-inseto-morto', 2, '12', 30],
        ['canoinha', 2, '30', 30]
      ] }
    ]
  }
];
