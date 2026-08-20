import React from 'react';

export const DashboardKPIs = ({ carregamentos = [] }) => {
  const totalCarregamentos = carregamentos.length;

  const totalApresentados = carregamentos.filter(
    (c) => c.status && c.status.toUpperCase() === 'APRESENTADO'
  ).length;

  const totalCarregando = carregamentos.filter(
    (c) => c.status && c.status.toUpperCase() === 'CARREGANDO'
  ).length;

  const totalExpedidos = carregamentos.filter(
    (c) => c.status && c.status.toUpperCase() === 'EXPEDIDO'
  ).length;

  const pesoTotal = carregamentos.reduce((acc, item) => {
    if (!item.peso) return acc;
    const num = parseFloat(
      item.peso.toString().replace(/[^\d.,]/g, '').replace(',', '.')
    );
    return acc + (isNaN(num) ? 0 : num);
  }, 0);

  const pesoTotalFormatado = pesoTotal.toLocaleString('pt-BR', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  });

  return (
    <div style={{ display: 'flex', gap: '1rem', width: '100%', marginBottom: '1.5rem', flexWrap: 'wrap' }}>
      
      {/* TOTAL CARREGAMENTOS */}
      <div style={{ flex: 1, minWidth: '180px', backgroundColor: '#1e293b', border: '1px solid #334155', borderTop: '4px solid #64748b', borderRadius: '8px', padding: '1rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.75rem', fontWeight: 700, color: '#94a3b8', letterSpacing: '0.05em' }}>
          <span>TOTAL CARREGAMENTOS</span>
          <span style={{ color: '#94a3b8', fontSize: '1.1rem' }}>🚛</span>
        </div>
        <div style={{ fontSize: '1.75rem', fontWeight: 800, color: '#ffffff', marginTop: '0.5rem' }}>
          {totalCarregamentos}
        </div>
      </div>

      {/* APRESENTADOS */}
      <div style={{ flex: 1, minWidth: '180px', backgroundColor: '#1e293b', border: '1px solid #334155', borderTop: '4px solid #2563eb', borderRadius: '8px', padding: '1rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.75rem', fontWeight: 700, color: '#94a3b8', letterSpacing: '0.05em' }}>
          <span>APRESENTADOS</span>
          <span style={{ color: '#2563eb', fontSize: '1.1rem' }}>☑️</span>
        </div>
        <div style={{ fontSize: '1.75rem', fontWeight: 800, color: '#ffffff', marginTop: '0.5rem' }}>
          {totalApresentados}
        </div>
      </div>

      {/* CARREGANDO */}
      <div style={{ flex: 1, minWidth: '180px', backgroundColor: '#1e293b', border: '1px solid #334155', borderTop: '4px solid #d97706', borderRadius: '8px', padding: '1rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.75rem', fontWeight: 700, color: '#94a3b8', letterSpacing: '0.05em' }}>
          <span>CARREGANDO</span>
          <span style={{ color: '#d97706', fontSize: '1.1rem' }}>🕒</span>
        </div>
        <div style={{ fontSize: '1.75rem', fontWeight: 800, color: '#ffffff', marginTop: '0.5rem' }}>
          {totalCarregando}
        </div>
      </div>

      {/* EXPEDIDOS */}
      <div style={{ flex: 1, minWidth: '180px', backgroundColor: '#1e293b', border: '1px solid #334155', borderTop: '4px solid #16a34a', borderRadius: '8px', padding: '1rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.75rem', fontWeight: 700, color: '#94a3b8', letterSpacing: '0.05em' }}>
          <span>EXPEDIDOS</span>
          <span style={{ color: '#16a34a', fontSize: '1.1rem' }}>📦</span>
        </div>
        <div style={{ fontSize: '1.75rem', fontWeight: 800, color: '#ffffff', marginTop: '0.5rem' }}>
          {totalExpedidos}
        </div>
      </div>

      {/* PESO PROGRAMADO */}
      <div style={{ flex: 1, minWidth: '180px', backgroundColor: '#1e293b', border: '1px solid #334155', borderTop: '4px solid #9333ea', borderRadius: '8px', padding: '1rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.75rem', fontWeight: 700, color: '#94a3b8', letterSpacing: '0.05em' }}>
          <span>PESO PROGRAMADO</span>
          <span style={{ color: '#9333ea', fontSize: '1.1rem' }}>⚖️</span>
        </div>
        <div style={{ fontSize: '1.75rem', fontWeight: 800, color: '#ffffff', marginTop: '0.5rem' }}>
          {pesoTotalFormatado} kg
        </div>
      </div>

    </div>
  );
};