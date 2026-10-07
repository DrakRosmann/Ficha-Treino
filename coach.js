/* Ficha — treinador com IA: relatório da semana e conversa com os seus dados */
'use strict';

/* ================= Dados enviados à IA ================= */
function coachData() {
  if (!S.coach) S.coach = { report: null, chat: [] };
  if (!S.coach.chat) S.coach.chat = [];
  return S.coach;
}
function cdate(t) { const d = new Date(t); return `${DAY[d.getDay()]} ${dateShort(t)}`; }
function topSetTxt(e) {
  const w = e.sets.filter(isWork);
  if (!w.length) return '—';
  if (e.kind === 'w') return w.map(s => `${fmt(s.a || 0)}×${s.b || 0}${s.rir != null ? ` RIR${s.rir}` : ''}${s.t ? ` (${{ drop: 'drop', fail: 'falha' }[s.t] || s.t})` : ''}`).join(', ');
  if (e.kind === 'bw') return w.map(s => `${s.b || 0}${s.a ? `+${fmt(s.a)}kg` : ''}`).join(', ');
  if (e.kind === 's') return w.map(s => `${s.a || 0}s`).join(', ');
  return w.map(s => `${s.a || 0}min${s.b ? ` ${fmt(s.b)}km` : ''}`).join(', ');
}
// Resumo compacto em texto: perfil, plano, treinos, evolução, dieta e corpo
function coachContext() {
  const now = Date.now(), today = startOfDay(now), L = [];
  const p = { ...(S.profile || {}) };
  const bw = typeof dietWeight === 'function' ? dietWeight() : num(p.peso);
  L.push(`Data de hoje: ${dateLong(now)} de ${new Date(now).getFullYear()}.`);
  L.push(`## Perfil\nSexo: ${p.sexo === 'm' ? 'masculino' : p.sexo === 'f' ? 'feminino' : 'não informado'} · Idade: ${p.idade || '?'} · Altura: ${p.altura || '?'} cm · Peso atual: ${bw ? fmt(bw) + ' kg' : '?'}${p.objetivo ? ` · Objetivo no assistente: ${p.objetivo}` : ''}${p.nivel ? ` · Nível: ${p.nivel}` : ''}${p.obs ? `\nObservações do usuário: ${p.obs}` : ''}`);

  // Plano atual
  const rs = S.routines.filter(r => r.items.length && (typeof isScheduled !== 'function' || isScheduled(r)));
  if (rs.length) {
    L.push('## Fichas em uso\n' + rs.map(r => `- ${r.name} (${daysLabel(r.days)}): ` + r.items.map((it, i) => {
      const g = typeof ssInfo === 'function' ? ssInfo(r.items, i) : null;
      return `${exName(it.exId)} ${it.sets}×${it.reps || '?'}${g ? ` [${g.name.toLowerCase()} ${g.letter}]` : ''}`;
    }).join('; ')).join('\n'));
  }

  // Treinos dos últimos 28 dias
  const recent = S.sessions.filter(s => s.start >= addDays(today, -28)).sort((a, b) => a.start - b.start);
  const wk = startOfWeek(now);
  L.push(`## Frequência\nTreinos por semana (da mais antiga para a atual): ${[3, 2, 1, 0].map(k => sessionsInRange(addDays(wk, -7 * k), addDays(wk, -7 * k + 7)).length).join(', ')} · Semanas seguidas treinando: ${typeof streakWeeks === 'function' ? streakWeeks() : '?'} · Total de treinos registrados: ${S.sessions.length}`);
  if (recent.length) {
    L.push('## Treinos dos últimos 28 dias (séries de trabalho: kg×reps)\n' + recent.map(s =>
      `- ${cdate(s.start)} ${timeHM(s.start)} · ${s.name} · ${fmtDur(s.end - s.start)}${s.prs && s.prs.length ? ` · recordes: ${s.prs.map(x => `${x.name} ${x.label} ${fmt(x.value)}${x.unit}`).join(', ')}` : ''}\n` +
      s.exercises.map(e => `  ${exName(e.exId, e.name)}: ${topSetTxt(e)}`).join('\n') + (s.notes ? `\n  Anotação: ${s.notes}` : '')).join('\n'));
  } else L.push('## Treinos dos últimos 28 dias\nNenhum treino registrado.');

  // Evolução dos principais exercícios (1RM estimado por semana, 8 semanas)
  const freq = new Map();
  for (const s of S.sessions.filter(s => s.start >= addDays(today, -56))) for (const e of s.exercises) if (e.kind === 'w') freq.set(e.exId, (freq.get(e.exId) || 0) + 1);
  const main = [...freq.entries()].sort((a, b) => b[1] - a[1]).slice(0, 8).map(x => x[0]);
  if (main.length) {
    L.push('## Evolução (melhor 1RM estimado por semana, 8 semanas, da mais antiga para a atual; "-" = não treinou)\n' + main.map(id => {
      const wks = [7, 6, 5, 4, 3, 2, 1, 0].map(k => {
        const a = addDays(wk, -7 * k), b = addDays(a, 7);
        let m = 0;
        for (const s of sessionsInRange(a, b)) for (const e of s.exercises) if (e.exId === id) for (const x of e.sets.filter(isWork)) m = Math.max(m, e1rm(x.a, x.b));
        return m ? fmt(m, 0) : '-';
      });
      return `- ${exName(id)}: ${wks.join(' → ')} kg`;
    }).join('\n'));
  }
  // Volume por músculo
  if (typeof groupSets === 'function' && typeof VOL_GROUPS !== 'undefined') {
    const cur = groupSets(wk, addDays(wk, 7)), avg = groupSets(addDays(wk, -28), wk).map(v => v / 4);
    L.push(`## Séries por grupo muscular (semana atual / média das 4 anteriores; referência ${VOL_RANGE ? VOL_RANGE.join('–') : '10–20'} por semana)\n` +
      VOL_GROUPS.map(([n], i) => cur[i] || avg[i] ? `${n}: ${fmt(cur[i], 1)} / ${fmt(avg[i], 1)}` : '').filter(Boolean).join(' · '));
  }

  // Dieta (14 dias)
  const D = S.food;
  if (D && D.days) {
    const tg = typeof dietTargets === 'function' ? dietTargets() : null;
    const rows = [];
    for (let i = 13; i >= 0; i--) {
      const t = addDays(today, -i), k = isoDate(t), d = D.days[k];
      if (!d || (!d.e.length && !d.w)) continue;
      const tot = dayTotals(k);
      rows.push(`- ${cdate(t)}: ${fmtInt(tot.k)} kcal · P ${fmt(tot.p, 0)} · C ${fmt(tot.c, 0)} · G ${fmt(tot.f, 0)} · fibras ${fmt(tot.fi, 0)} g · água ${fmt((d.w || 0) / 1000, 1)} L${i === 0 ? ' (hoje, dia em andamento)' : ''}`);
    }
    const g = typeof dietGoal === 'function' && D.goal ? dietGoal() : null;
    L.push(`## Dieta\n${g ? `Objetivo: ${{ perder: 'perder gordura', manter: 'manter', ganhar: 'ganhar massa' }[g.obj]}${g.obj !== 'manter' ? ` (${g.rate}% do peso por semana)` : ''}` : 'Metas não definidas'}` +
      `${tg ? ` · Meta diária: ${fmtInt(tg.k)} kcal, P ${tg.p} g, C ${tg.c} g, G ${tg.f} g` : ''}` +
      `${typeof currentTDEE === 'function' && currentTDEE() ? ` · Gasto estimado (TDEE): ${fmtInt(currentTDEE())} kcal${D.tdee && D.tdee.v ? ' (ajustado pelos registros)' : ' (fórmula)'}` : ''}` +
      (rows.length ? '\nÚltimos 14 dias:\n' + rows.join('\n') : '\nSem registros de alimentação nos últimos 14 dias.'));
  }
  // Corpo
  const body = (S.body || []).slice().sort((a, b) => a.t - b.t);
  if (body.length) {
    const pesos = body.filter(e => e.peso && e.t >= addDays(today, -56));
    const slope = typeof weightSlope === 'function' ? weightSlope(28) : null;
    let txt = '## Corpo\n';
    if (pesos.length) txt += `Pesagens (8 semanas): ${pesos.map(e => `${dateShort(e.t)} ${fmt(e.peso)}`).join(', ')}\n`;
    if (slope != null) txt += `Tendência do peso (regressão, 28 dias): ${slope >= 0 ? '+' : ''}${fmt(slope * 7, 2)} kg/semana\n`;
    if (typeof BODY_FIELDS !== 'undefined') {
      const last = body[body.length - 1];
      const meds = BODY_FIELDS.filter(f => f[0] !== 'peso' && last[f[0]] != null).map(f => {
        const first = body.find(e => e[f[0]] != null);
        return `${f[1]} ${fmt(last[f[0]], f[5])}${f[2]}${first !== last ? ` (era ${fmt(first[f[0]], f[5])} em ${dateShort(first.t)})` : ''}`;
      });
      if (meds.length) txt += `Últimas medidas (${dateShort(last.t)}): ${meds.join(', ')}`;
    }
    L.push(txt.trim());
  }
  if ((S.photos || []).length) L.push(`## Fotos do progresso\n${S.photos.length} fotos, a mais recente em ${dateShort(max(S.photos.map(x => x.t)))}.`);
  return L.join('\n\n');
}

const COACH_SYSTEM = `Você é o treinador do app Ficha, um app brasileiro de registro de treino de musculação e dieta. Fala em português do Brasil, de forma direta, motivadora e prática, como um bom personal trainer e nutricionista esportivo.

Regras:
- Baseie-se nos dados do usuário abaixo. Cite números concretos (cargas, séries, calorias, proteína, peso) quando ajudar.
- Use evidência atual de treino de força e nutrição: sobrecarga progressiva, 10–20 séries por músculo por semana, 1,6–2,2 g/kg de proteína, déficit ou superávit moderado, sono e recuperação.
- Se faltar dado para uma conclusão, diga o que falta registrar, sem inventar.
- Nada de diagnóstico médico. Com dor, lesão ou sintomas, recomende procurar um profissional de saúde.
- Respostas curtas e escaneáveis. Use listas curtas e **negrito** só no essencial.`;

/* ================= Chamada à IA ================= */
const COACH_SCHEMA = {
  type: 'object', additionalProperties: false, required: ['resumo', 'destaques', 'atencao', 'recomendacoes', 'metas_semana'],
  properties: {
    resumo: { type: 'string', description: 'Duas ou três frases sobre a semana' },
    destaques: { type: 'array', items: { type: 'string' }, description: 'O que foi bem (até 4)' },
    atencao: { type: 'array', items: { type: 'string' }, description: 'Pontos de atenção (até 4)' },
    recomendacoes: { type: 'array', items: { type: 'object', additionalProperties: false, required: ['titulo', 'detalhe'], properties: { titulo: { type: 'string' }, detalhe: { type: 'string' } } }, description: 'Ações concretas para a próxima semana (até 4)' },
    metas_semana: { type: 'array', items: { type: 'string' }, description: 'Metas objetivas e mensuráveis para a semana (até 3)' }
  }
};
// Conversa no formato da API: começa pelo usuário e alterna os papéis
function coachMessages(history) {
  const out = [];
  for (const m of history) {
    const role = m.r === 'u' ? 'user' : 'assistant';
    if (!out.length && role !== 'user') continue;
    if (out.length && out[out.length - 1].role === role) out[out.length - 1].content += '\n\n' + m.x;
    else out.push({ role, content: m.x });
  }
  return out;
}
async function coachCall({ report, history, onText, holder }) {
  const Anthropic = await aiSdk();
  const client = new Anthropic({ apiKey: aiKey(), dangerouslyAllowBrowser: true, maxRetries: 1 });
  const ctx = coachContext();
  const params = {
    model: ASST_AI_MODEL,
    max_tokens: report ? 16000 : 8000,
    // Se o modelo recusar o pedido, a API tenta de novo num modelo alternativo recomendado
    betas: ['server-side-fallback-2026-07-01'],
    fallbacks: 'default',
    cache_control: { type: 'ephemeral' }, // os dados (system) e a conversa ficam em cache entre as mensagens
    system: `${COACH_SYSTEM}\n\n# Dados do usuário\n${ctx}`,
    output_config: report ? { effort: 'medium', format: { type: 'json_schema', schema: COACH_SCHEMA } } : { effort: 'low' },
    messages: report
      ? [{ role: 'user', content: 'Faça o relatório da minha semana de treino e dieta: compare com as semanas anteriores e diga o que fazer na próxima.' }]
      : coachMessages(history)
  };
  try {
    const stream = client.beta.messages.stream(params);
    if (holder) holder.stream = stream;
    if (onText) stream.on('text', d => onText(d));
    const msg = await stream.finalMessage();
    if (msg.stop_reason === 'refusal') throw new AiError('A IA não conseguiu responder isto. Tente perguntar de outro jeito.');
    if (msg.stop_reason === 'max_tokens' && report) throw new AiError('A resposta ficou incompleta. Tente de novo.');
    const text = msg.content.filter(b => b.type === 'text').map(b => b.text).join('');
    if (!report) return text;
    try { return JSON.parse(text); } catch (e) { throw new AiError('A resposta da IA veio num formato inesperado. Tente de novo.'); }
  } catch (e) {
    throw aiFriendly(e, Anthropic);
  }
}

/* ================= Texto com formatação simples ================= */
function mdLite(src) {
  const inline = s => esc(s).replace(/\*\*(.+?)\*\*/g, '<b>$1</b>').replace(/(^|[^*])\*(?!\s)(.+?)\*(?!\*)/g, '$1<i>$2</i>').replace(/`([^`]+)`/g, '<code>$1</code>');
  const out = []; let list = null;
  const flush = () => { if (list) { out.push(`<${list.t}>${list.items.map(x => `<li>${x}</li>`).join('')}</${list.t}>`); list = null; } };
  for (const raw of String(src).split('\n')) {
    const line = raw.trimEnd();
    let m;
    if ((m = line.match(/^\s*[-*•]\s+(.*)$/))) { if (!list || list.t !== 'ul') { flush(); list = { t: 'ul', items: [] }; } list.items.push(inline(m[1])); continue; }
    if ((m = line.match(/^\s*\d+[.)]\s+(.*)$/))) { if (!list || list.t !== 'ol') { flush(); list = { t: 'ol', items: [] }; } list.items.push(inline(m[1])); continue; }
    flush();
    if (!line.trim()) continue;
    if ((m = line.match(/^#{1,4}\s+(.*)$/))) out.push(`<p class="md-h">${inline(m[1])}</p>`);
    else out.push(`<p>${inline(line)}</p>`);
  }
  flush();
  return out.join('');
}

/* ================= Telas ================= */
let coachRun = null; // { holder, kind, text }
function coachWeekKey(t = Date.now()) { return isoDate(startOfWeek(t)); }
function coachTodayCard() {
  if (S.active) return '';
  const C = coachData(), rep = C.report, fresh = rep && rep.week === coachWeekKey();
  const sub = fresh ? `Relatório desta semana · ${esc((rep.data.metas_semana || [])[0] || 'veja as metas')}` : S.sessions.length ? 'Gere o relatório da semana ou pergunte sobre seu treino e dieta' : 'Tire dúvidas de treino e dieta com base nos seus dados';
  return `<a class="tpl-banner coach-banner" href="#/treinador"><span class="tpl-ic">${I.sparkle}</span><div class="grow"><b>Treinador IA</b><span>${sub}</span></div>${I.chev}</a>`;
}
function reportHTML(rep) {
  const d = rep.data, li = a => (a || []).map(x => `<li>${esc(x)}</li>`).join('');
  return `<div class="card coach-rep">
    <div class="small muted" style="font-weight:700">RELATÓRIO · SEMANA DE ${dateShort(new Date(rep.week + 'T12:00').getTime()).toUpperCase()} · gerado ${relDay(rep.at).toLowerCase()} ${timeHM(rep.at)}</div>
    <p class="coach-sum">${esc(d.resumo)}</p>
    ${d.destaques && d.destaques.length ? `<div class="coach-blk ok"><b>O que foi bem</b><ul>${li(d.destaques)}</ul></div>` : ''}
    ${d.atencao && d.atencao.length ? `<div class="coach-blk warn"><b>Atenção</b><ul>${li(d.atencao)}</ul></div>` : ''}
    ${d.recomendacoes && d.recomendacoes.length ? `<div class="coach-blk"><b>Para a próxima semana</b>${d.recomendacoes.map(r => `<div class="coach-rec"><b>${esc(r.titulo)}</b><span>${esc(r.detalhe)}</span></div>`).join('')}</div>` : ''}
    ${d.metas_semana && d.metas_semana.length ? `<div class="coach-blk goal"><b>Metas da semana</b><ul class="coach-goals">${li(d.metas_semana)}</ul></div>` : ''}
  </div>`;
}
const COACH_SUGGEST = ['Meu treino está bom para o meu objetivo?', 'Por que minha carga parou de subir?', 'Estou comendo proteína suficiente?', 'O que ajustar na dieta esta semana?', 'Monte um aquecimento para o treino de hoje'];
function viewTreinador() {
  const C = coachData(), rep = C.report, run = coachRun;
  let html = topBar({ back: '#/hoje', right: C.chat.length ? `<button class="link-btn" data-act="coachClear">Limpar conversa</button>` : '' }) +
    `<div class="eyebrow">Com base nos seus registros</div><h1 class="title">Treinador IA</h1>`;
  if (run && run.kind === 'report') html += `<div class="card coach-rep"><div class="ai-run" style="padding:10px 0"><div class="ai-orb">${I.sparkle}</div><h3>Analisando sua semana…</h3><p class="muted small">Treinos, cargas, dieta e peso. Leva alguns segundos.</p></div></div>`;
  else if (rep) html += reportHTML(rep);
  else html += `<div class="card coach-empty"><b>Relatório da semana</b><p class="small muted">A IA lê seus treinos, cargas, volume por músculo, dieta e peso, compara com as semanas anteriores e diz o que ajustar.</p></div>`;
  if (!(run && run.kind === 'report')) html += `<button class="btn block ${rep && rep.week === coachWeekKey() ? '' : 'primary'}" data-act="coachReport" style="margin-top:4px">${I.sparkle}${rep ? 'Gerar relatório de novo' : 'Gerar relatório da semana'}</button>`;

  html += `<h2 class="section">Conversa</h2><div class="coach-chat" id="coachChat">`;
  if (!C.chat.length && !(run && run.kind === 'chat')) html += `<p class="small muted" style="margin:0 4px 10px">Pergunte qualquer coisa sobre o seu treino, dieta ou evolução. A IA vê os seus registros.</p>`;
  html += C.chat.map(m => `<div class="msg ${m.r === 'u' ? 'me' : 'ai'}">${m.r === 'u' ? esc(m.x).replace(/\n/g, '<br>') : mdLite(m.x)}</div>`).join('');
  if (run && run.kind === 'chat') html += `<div class="msg ai" id="coachLive">${run.text ? mdLite(run.text) : '<span class="typing"><i></i><i></i><i></i></span>'}</div>`;
  html += `</div>`;
  if (!C.chat.length) html += `<div class="chips wrap coach-sugg">${COACH_SUGGEST.map(q => `<button class="chip" data-act="coachAsk" data-q="${esc(q)}">${esc(q)}</button>`).join('')}</div>`;
  html += `<div class="coach-input"><textarea class="input" id="coachQ" rows="1" placeholder="Pergunte ao treinador…" ${run ? 'disabled' : ''}></textarea>
    ${run && run.kind === 'chat' ? `<button class="icon-btn send" data-act="coachStop" aria-label="Parar">${I.x}</button>` : `<button class="icon-btn send" data-act="coachSend" aria-label="Enviar">${I.up}</button>`}</div>
    <p class="small muted" style="margin:10px 4px 0;line-height:1.5">Usa o Claude com a sua chave da API (cobrança por uso na sua conta da Anthropic). Ao gerar ou perguntar, um resumo dos seus registros é enviado à Anthropic. Não substitui um profissional de educação física, nutricionista ou médico.</p>`;
  return html;
}
function coachReady() {
  if (typeof aiKey !== 'function' || typeof coachCall !== 'function') { toast('Atualize o app para usar a IA'); return false; }
  if (!aiKey()) { aiKeySheet(''); toast('Salve a chave da API e tente de novo'); return false; }
  if (!navigator.onLine) { toast('Sem internet — a IA precisa de conexão'); return false; }
  return true;
}
function coachRerender() { if (location.hash === '#/treinador') rerender(); }
function coachScroll() { const el = $('#coachLive') || $('#coachChat .msg:last-child'); if (el) el.scrollIntoView({ block: 'end', behavior: 'smooth' }); }
async function coachSend(q) {
  q = String(q || '').trim();
  if (!q || coachRun || !coachReady()) return;
  const C = coachData();
  C.chat.push({ r: 'u', x: q, at: Date.now() });
  C.chat = C.chat.slice(-40);
  save();
  const run = coachRun = { kind: 'chat', text: '', holder: {} };
  coachRerender(); coachScroll();
  let raf = 0;
  try {
    const text = await coachCall({
      history: C.chat.slice(-20), holder: run.holder,
      onText: d => {
        run.text += d;
        if (!raf) raf = requestAnimationFrame(() => { raf = 0; const el = $('#coachLive'); if (el) { el.innerHTML = mdLite(run.text); } });
      }
    });
    if (coachRun !== run) return;
    C.chat.push({ r: 'a', x: text.trim() || '(sem resposta)', at: Date.now() });
  } catch (e) {
    if (coachRun !== run) return;
    if (run.text.trim()) C.chat.push({ r: 'a', x: run.text.trim() + '\n\n*(resposta interrompida)*', at: Date.now() });
    else if (!run.stopped) toast(e instanceof AiError ? e.message : 'Não foi possível usar a IA agora');
  }
  coachRun = null; save(); coachRerender(); setTimeout(coachScroll, 50);
}
async function coachReport() {
  if (coachRun || !coachReady()) return;
  const run = coachRun = { kind: 'report', holder: {} };
  coachRerender();
  try {
    const data = await coachCall({ report: true, holder: run.holder });
    if (coachRun !== run) return;
    coachData().report = { at: Date.now(), week: coachWeekKey(), data };
  } catch (e) {
    if (coachRun === run) toast(e instanceof AiError ? e.message : 'Não foi possível gerar o relatório agora');
  }
  if (coachRun === run) { coachRun = null; save(); coachRerender(); }
}
function coachInput(t) {
  if (t.id !== 'coachQ') return false;
  t.style.height = 'auto'; t.style.height = Math.min(140, t.scrollHeight) + 'px';
  return true;
}
document.addEventListener('keydown', e => {
  if (e.target.id === 'coachQ' && e.key === 'Enter' && !e.shiftKey && !e.isComposing && matchMedia('(hover: hover)').matches) { e.preventDefault(); coachSend(e.target.value); }
});

const COACH_ACTIONS = {
  coachReport: () => coachReport(),
  coachSend: () => { const t = $('#coachQ'); if (t) coachSend(t.value); },
  coachAsk: el => coachSend(el.dataset.q),
  coachStop: () => { if (coachRun && coachRun.holder.stream) { coachRun.stopped = true; coachRun.holder.stream.abort(); } },
  coachClear: () => confirmSheet({ title: 'Limpar conversa?', text: 'As mensagens com o treinador são apagadas. O relatório da semana fica.', ok: 'Limpar', danger: true, onOk: () => { coachData().chat = []; save(); rerender(); } })
};
