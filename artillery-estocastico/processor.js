'use strict';

const SYMBOLS = ['AAPL', 'GOOGL', 'MSFT', 'AMZN', 'TSLA', 'META', 'NVDA'];
const ORDER_TYPES = ['SALE', 'BUY'];

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function poissonRandom(lambda) {
  const L = Math.exp(-lambda);
  let p = 1.0;
  let k = 0;

  do {
    k++;
    p *= Math.random();
  } while (p > L);

  return k - 1;
}

module.exports = {

  initPoisson: function (context, events, done) {

    context.vars.inicio = Date.now();
    context.vars.duracion = 60000;
    context.vars.totalObjetivo = 800;
    context.vars.totalEnviadas = 0;
    context.vars.oleadaRestante = 0;
    context.vars.forzarFinal = false;

    return done();
  },

  configurarOleada: function (context, events, done) {

    const ahora = Date.now();

    if (!context.vars.forzarFinal &&
        ahora - context.vars.inicio >= context.vars.duracion) {

      const faltantes = context.vars.totalObjetivo - context.vars.totalEnviadas;

      if (faltantes > 0) {
        context.vars.oleadaRestante = faltantes;
        context.vars.forzarFinal = true;
      } else {
        return done(new Error('STOP'));
      }
    }

    if (context.vars.totalEnviadas >= context.vars.totalObjetivo) {
      return done(new Error('STOP'));
    }

    if (!context.vars.forzarFinal && context.vars.oleadaRestante === 0) {
      const restante = context.vars.totalObjetivo - context.vars.totalEnviadas;
      const tamOleada = randomInt(1, Math.min(50, restante));
      context.vars.oleadaRestante = tamOleada;
    }
    
    const orderType = ORDER_TYPES[randomInt(0, ORDER_TYPES.length - 1)];
    const symbol = SYMBOLS[randomInt(0, SYMBOLS.length - 1)];

    context.vars.endpoint =
      orderType === 'SALE'
        ? '/api/v1/orders/sale'
        : '/api/v1/orders/buy';

    context.vars.payload = {
      symbol: symbol,
      quantity: randomInt(1, 500),
      price: (Math.random() * 300 + 50).toFixed(2),
      type: orderType,
      userId: `user-${randomInt(1, 10000)}`
    };

    context.vars.totalEnviadas++;
    context.vars.oleadaRestante--;

    if (context.vars.forzarFinal && context.vars.oleadaRestante === 0) {
      return done(new Error('STOP'));
    }

    const intervalo = context.vars.forzarFinal
      ? 0
      : poissonRandom(2) * randomInt(1, 50);

    setTimeout(() => done(), intervalo);
  }

};
