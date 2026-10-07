// Catálogo de exercícios embutido.
// k = tipo de registro: w = carga × reps | bw = peso corporal (+kg opcional) × reps
//                       s = tempo em segundos | c = cardio (min / km)
'use strict';

const GROUPS = [
  'Peito', 'Costas', 'Ombros', 'Bíceps', 'Tríceps', 'Antebraço',
  'Quadríceps', 'Posterior de coxa', 'Glúteos', 'Panturrilha', 'Abdômen', 'Cardio'
];

const EQUIPMENT = ['Barra', 'Halteres', 'Máquina', 'Polia', 'Peso corporal', 'Smith', 'Kettlebell', 'Elástico', 'Outro'];

const KINDS = {
  w:  { label: 'Carga × repetições', a: 'kg',  b: 'reps' },
  bw: { label: 'Peso corporal × repetições', a: '+kg', b: 'reps' },
  s:  { label: 'Tempo (segundos)', a: 'seg', b: null },
  c:  { label: 'Cardio (minutos / km)', a: 'min', b: 'km' }
};

const BUILTIN_EXERCISES = [
  // Peito
  ['supino-reto-barra', 'Supino reto com barra', 'Peito', 'Barra'],
  ['supino-reto-halter', 'Supino reto com halteres', 'Peito', 'Halteres'],
  ['supino-inclinado-barra', 'Supino inclinado com barra', 'Peito', 'Barra'],
  ['supino-inclinado-halter', 'Supino inclinado com halteres', 'Peito', 'Halteres'],
  ['supino-inclinado-smith', 'Supino inclinado no Smith', 'Peito', 'Smith'],
  ['supino-declinado-barra', 'Supino declinado com barra', 'Peito', 'Barra'],
  ['supino-maquina', 'Supino na máquina', 'Peito', 'Máquina'],
  ['crucifixo-reto', 'Crucifixo reto com halteres', 'Peito', 'Halteres'],
  ['crucifixo-inclinado', 'Crucifixo inclinado com halteres', 'Peito', 'Halteres'],
  ['peck-deck', 'Crucifixo máquina (peck deck)', 'Peito', 'Máquina'],
  ['crossover-alto', 'Crossover polia alta', 'Peito', 'Polia'],
  ['crossover-baixo', 'Crossover polia baixa', 'Peito', 'Polia'],
  ['pullover', 'Pullover com halter', 'Peito', 'Halteres'],
  ['flexao', 'Flexão de braço', 'Peito', 'Peso corporal', 'bw'],
  ['mergulho-peito', 'Mergulho nas paralelas (peito)', 'Peito', 'Peso corporal', 'bw'],

  // Costas
  ['barra-fixa', 'Barra fixa (pronada)', 'Costas', 'Peso corporal', 'bw'],
  ['chin-up', 'Barra fixa supinada (chin-up)', 'Costas', 'Peso corporal', 'bw'],
  ['puxada-aberta', 'Puxada frontal aberta', 'Costas', 'Polia'],
  ['puxada-triangulo', 'Puxada frontal com triângulo', 'Costas', 'Polia'],
  ['puxada-supinada', 'Puxada supinada', 'Costas', 'Polia'],
  ['remada-curvada', 'Remada curvada com barra', 'Costas', 'Barra'],
  ['remada-curvada-supinada', 'Remada curvada supinada', 'Costas', 'Barra'],
  ['serrote', 'Remada unilateral com halter (serrote)', 'Costas', 'Halteres'],
  ['remada-baixa', 'Remada baixa sentada', 'Costas', 'Polia'],
  ['remada-cavalinho', 'Remada cavalinho (T-bar)', 'Costas', 'Barra'],
  ['remada-maquina', 'Remada na máquina', 'Costas', 'Máquina'],
  ['pulldown', 'Pulldown com braços estendidos', 'Costas', 'Polia'],
  ['terra', 'Levantamento terra', 'Costas', 'Barra'],
  ['hiperextensao', 'Hiperextensão lombar', 'Costas', 'Peso corporal', 'bw'],
  ['encolhimento-barra', 'Encolhimento com barra', 'Costas', 'Barra'],
  ['encolhimento-halter', 'Encolhimento com halteres', 'Costas', 'Halteres'],

  // Ombros
  ['desenvolvimento-barra', 'Desenvolvimento com barra (militar)', 'Ombros', 'Barra'],
  ['desenvolvimento-halter', 'Desenvolvimento com halteres', 'Ombros', 'Halteres'],
  ['desenvolvimento-maquina', 'Desenvolvimento na máquina', 'Ombros', 'Máquina'],
  ['arnold', 'Desenvolvimento Arnold', 'Ombros', 'Halteres'],
  ['elevacao-lateral', 'Elevação lateral com halteres', 'Ombros', 'Halteres'],
  ['elevacao-lateral-polia', 'Elevação lateral na polia', 'Ombros', 'Polia'],
  ['elevacao-lateral-maquina', 'Elevação lateral na máquina', 'Ombros', 'Máquina'],
  ['elevacao-frontal', 'Elevação frontal com halteres', 'Ombros', 'Halteres'],
  ['elevacao-frontal-anilha', 'Elevação frontal com anilha', 'Ombros', 'Outro'],
  ['crucifixo-invertido', 'Crucifixo invertido com halteres', 'Ombros', 'Halteres'],
  ['voador-inverso', 'Crucifixo invertido na máquina', 'Ombros', 'Máquina'],
  ['face-pull', 'Face pull', 'Ombros', 'Polia'],
  ['remada-alta', 'Remada alta', 'Ombros', 'Barra'],

  // Bíceps
  ['rosca-direta', 'Rosca direta com barra', 'Bíceps', 'Barra'],
  ['rosca-w', 'Rosca direta com barra W', 'Bíceps', 'Barra'],
  ['rosca-alternada', 'Rosca alternada com halteres', 'Bíceps', 'Halteres'],
  ['rosca-martelo', 'Rosca martelo', 'Bíceps', 'Halteres'],
  ['rosca-concentrada', 'Rosca concentrada', 'Bíceps', 'Halteres'],
  ['rosca-scott', 'Rosca Scott', 'Bíceps', 'Barra'],
  ['rosca-polia', 'Rosca na polia', 'Bíceps', 'Polia'],
  ['rosca-inclinada', 'Rosca no banco inclinado', 'Bíceps', 'Halteres'],
  ['rosca-maquina', 'Rosca bíceps na máquina', 'Bíceps', 'Máquina'],

  // Tríceps
  ['triceps-pulley', 'Tríceps pulley (barra)', 'Tríceps', 'Polia'],
  ['triceps-corda', 'Tríceps corda', 'Tríceps', 'Polia'],
  ['triceps-unilateral', 'Tríceps unilateral na polia', 'Tríceps', 'Polia'],
  ['triceps-testa', 'Tríceps testa', 'Tríceps', 'Barra'],
  ['triceps-frances', 'Tríceps francês', 'Tríceps', 'Halteres'],
  ['triceps-coice', 'Tríceps coice (kickback)', 'Tríceps', 'Halteres'],
  ['supino-fechado', 'Supino fechado', 'Tríceps', 'Barra'],
  ['mergulho-banco', 'Mergulho no banco', 'Tríceps', 'Peso corporal', 'bw'],
  ['paralelas', 'Paralelas (tríceps)', 'Tríceps', 'Peso corporal', 'bw'],

  // Antebraço
  ['rosca-punho', 'Rosca de punho', 'Antebraço', 'Barra'],
  ['rosca-punho-invertida', 'Rosca de punho invertida', 'Antebraço', 'Barra'],
  ['rosca-inversa', 'Rosca inversa', 'Antebraço', 'Barra'],

  // Quadríceps
  ['agachamento', 'Agachamento livre', 'Quadríceps', 'Barra'],
  ['agachamento-frontal', 'Agachamento frontal', 'Quadríceps', 'Barra'],
  ['agachamento-smith', 'Agachamento no Smith', 'Quadríceps', 'Smith'],
  ['hack', 'Agachamento hack', 'Quadríceps', 'Máquina'],
  ['goblet', 'Agachamento goblet', 'Quadríceps', 'Kettlebell'],
  ['bulgaro', 'Agachamento búlgaro', 'Quadríceps', 'Halteres'],
  ['leg-press-45', 'Leg press 45°', 'Quadríceps', 'Máquina'],
  ['leg-press-horizontal', 'Leg press horizontal', 'Quadríceps', 'Máquina'],
  ['extensora', 'Cadeira extensora', 'Quadríceps', 'Máquina'],
  ['passada', 'Afundo / passada', 'Quadríceps', 'Halteres'],
  ['step-up', 'Subida no banco (step-up)', 'Quadríceps', 'Halteres'],

  // Posterior de coxa
  ['mesa-flexora', 'Mesa flexora', 'Posterior de coxa', 'Máquina'],
  ['cadeira-flexora', 'Cadeira flexora', 'Posterior de coxa', 'Máquina'],
  ['flexora-em-pe', 'Flexora em pé (unilateral)', 'Posterior de coxa', 'Máquina'],
  ['stiff-barra', 'Stiff com barra', 'Posterior de coxa', 'Barra'],
  ['stiff-halter', 'Stiff com halteres', 'Posterior de coxa', 'Halteres'],
  ['terra-romeno', 'Levantamento terra romeno', 'Posterior de coxa', 'Barra'],
  ['good-morning', 'Good morning', 'Posterior de coxa', 'Barra'],
  ['flexao-nordica', 'Flexão nórdica', 'Posterior de coxa', 'Peso corporal', 'bw'],

  // Glúteos
  ['hip-thrust', 'Elevação pélvica (hip thrust)', 'Glúteos', 'Barra'],
  ['ponte-gluteo', 'Ponte de glúteo', 'Glúteos', 'Peso corporal', 'bw'],
  ['gluteo-polia', 'Glúteo coice na polia', 'Glúteos', 'Polia'],
  ['gluteo-maquina', 'Glúteo na máquina', 'Glúteos', 'Máquina'],
  ['abdutora', 'Cadeira abdutora', 'Glúteos', 'Máquina'],
  ['adutora', 'Cadeira adutora', 'Glúteos', 'Máquina'],
  ['abducao-polia', 'Abdução de quadril na polia', 'Glúteos', 'Polia'],

  // Panturrilha
  ['panturrilha-em-pe', 'Panturrilha em pé', 'Panturrilha', 'Máquina'],
  ['panturrilha-sentado', 'Panturrilha sentado', 'Panturrilha', 'Máquina'],
  ['panturrilha-leg', 'Panturrilha no leg press', 'Panturrilha', 'Máquina'],
  ['panturrilha-smith', 'Panturrilha no Smith', 'Panturrilha', 'Smith'],

  // Abdômen
  ['abdominal-supra', 'Abdominal supra (crunch)', 'Abdômen', 'Peso corporal', 'bw'],
  ['abdominal-infra', 'Abdominal infra', 'Abdômen', 'Peso corporal', 'bw'],
  ['abdominal-bicicleta', 'Abdominal bicicleta', 'Abdômen', 'Peso corporal', 'bw'],
  ['elevacao-pernas', 'Elevação de pernas na barra', 'Abdômen', 'Peso corporal', 'bw'],
  ['abdominal-polia', 'Abdominal na polia', 'Abdômen', 'Polia'],
  ['abdominal-maquina', 'Abdominal na máquina', 'Abdômen', 'Máquina'],
  ['roda-abdominal', 'Roda abdominal', 'Abdômen', 'Outro', 'bw'],
  ['russian-twist', 'Russian twist', 'Abdômen', 'Peso corporal', 'bw'],
  ['prancha', 'Prancha', 'Abdômen', 'Peso corporal', 's'],
  ['prancha-lateral', 'Prancha lateral', 'Abdômen', 'Peso corporal', 's'],

  // Cardio
  ['esteira', 'Esteira', 'Cardio', 'Máquina', 'c'],
  ['bicicleta', 'Bicicleta ergométrica', 'Cardio', 'Máquina', 'c'],
  ['eliptico', 'Elíptico', 'Cardio', 'Máquina', 'c'],
  ['escada', 'Simulador de escada', 'Cardio', 'Máquina', 'c'],
  ['remo-ergometrico', 'Remo ergométrico', 'Cardio', 'Máquina', 'c'],
  ['pular-corda', 'Pular corda', 'Cardio', 'Outro', 'c']
].map(([id, name, group, equip, kind]) => ({ id, name, group, equip, kind: kind || 'w' }));
