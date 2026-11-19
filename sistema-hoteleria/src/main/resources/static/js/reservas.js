'use strict';

const $ = (id) => document.getElementById(id);

const API = document.body.dataset.apiBase;
const HUESPEDES_API = document.body.dataset.huespedesApi;

const panel = $('panelReserva');
const formulario = $('formReserva');

const formatoNumero = new Intl.NumberFormat('es', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
});

const nombresEstado = {
    PENDIENTE: 'Pendiente',
    CONFIRMADA: 'Confirmada',
    CANCELADA: 'Cancelada',
    FINALIZADA: 'Finalizada'
};

const clasesEstado = {
    PENDIENTE: 'mantenimiento',
    CONFIRMADA: 'habilitada',
    CANCELADA: 'inactiva',
    FINALIZADA: 'inactiva'
};

let reservas = [];
let habitacionesDisponibles = new Map();

let fechasConsultadas = null;
let numeroConsulta = 0;
let numeroDetalle = 0;

let guardando = false;
let consultando = false;
let huespedesCargados = false;

document.querySelector('[data-menu="reservas"]')
    ?.classList.add('activo');

function mostrarMensaje(texto, error = false) {
    $('mensaje').textContent = texto;
    $('mensaje').className = `mensaje ${error ? 'error' : 'exito'}`;
    $('mensaje').hidden = false;
}

function mostrarErrorFormulario(texto) {
    $('errorFormulario').textContent = texto;
    $('errorFormulario').hidden = false;
}

async function peticion(url, opciones = {}) {
    const respuesta = await fetch(url, opciones);
    const texto = await respuesta.text();

    let datos = texto;

    if (texto) {
        try {
            datos = JSON.parse(texto);
        } catch {
            // Algunos endpoints responden errores como texto.
        }
    }

    if (!respuesta.ok) {
        let mensaje =
            `No se pudo completar la operación (${respuesta.status}).`;

        if (typeof datos === 'string' && datos.trim()) {
            if (!datos.trim().startsWith('<')) {
                mensaje = datos;
            }
        } else if (datos && typeof datos === 'object') {
            if (typeof datos.detail === 'string') {
                mensaje = datos.detail;
            } else if (typeof datos.message === 'string') {
                mensaje = datos.message;
            } else {
                const mensajes = Object.values(datos)
                    .filter((valor) => typeof valor === 'string');

                if (mensajes.length) {
                    mensaje = mensajes.join(' · ');
                }
            }
        }

        throw new Error(mensaje);
    }

    return datos;
}

function importe(valor) {
    return valor == null
        ? '—'
        : formatoNumero.format(Number(valor));
}

function fechaVisible(fecha) {
    if (!fecha) {
        return '—';
    }

    const [anio, mes, dia] = fecha.split('-');
    return `${dia}/${mes}/${anio}`;
}

function nochesEntre(entrada, salida) {
    const inicio = Date.parse(`${entrada}T00:00:00Z`);
    const fin = Date.parse(`${salida}T00:00:00Z`);

    return Math.round((fin - inicio) / 86400000);
}

function celda(fila, texto) {
    const elemento = document.createElement('td');

    elemento.textContent = texto ?? '—';
    fila.appendChild(elemento);

    return elemento;
}

function boton(texto, clase, accion) {
    const elemento = document.createElement('button');

    elemento.type = 'button';
    elemento.className = clase;
    elemento.textContent = texto;
    elemento.addEventListener('click', accion);

    return elemento;
}

function filaInformativa(texto) {
    const cuerpo = $('tablaReservas');
    cuerpo.replaceChildren();

    const fila = document.createElement('tr');
    const elemento = celda(fila, texto);

    elemento.colSpan = 8;
    elemento.className = 'tabla-vacia';

    cuerpo.appendChild(fila);
}

function renderizarReservas() {
    const busqueda = $('buscar').value
        .trim()
        .toLocaleLowerCase('es');

    const estado = $('filtroEstado').value;

    const filtradas = reservas.filter((reserva) => {
        const texto = [
            reserva.id,
            `RES-${reserva.id}`,
            reserva.habitacion?.numero
        ].join(' ').toLocaleLowerCase('es');

        return texto.includes(busqueda)
            && (!estado || reserva.estado === estado);
    });

    $('totalReservas').textContent = reservas.length;

    $('pendientes').textContent = reservas.filter(
        (r) => r.estado === 'PENDIENTE'
    ).length;

    $('confirmadas').textContent = reservas.filter(
        (r) => r.estado === 'CONFIRMADA'
    ).length;

    $('canceladas').textContent = reservas.filter(
        (r) => r.estado === 'CANCELADA'
    ).length;

    $('cantidadResultados').textContent =
        `${filtradas.length} de ${reservas.length} reservas`;

    if (!filtradas.length) {
        filaInformativa(
            reservas.length
                ? 'No hay resultados para estos filtros.'
                : 'Todavía no hay reservas registradas.'
        );
        return;
    }

    const cuerpo = $('tablaReservas');
    cuerpo.replaceChildren();

    filtradas.forEach((reserva) => {
        const fila = document.createElement('tr');

        celda(fila, `RES-${reserva.id}`);
        celda(fila, reserva.habitacion?.numero);
        celda(fila, fechaVisible(reserva.fechaEntrada));
        celda(fila, fechaVisible(reserva.fechaSalida));

        celda(
            fila,
            nochesEntre(reserva.fechaEntrada, reserva.fechaSalida)
        );

        celda(fila, importe(reserva.total));

        const estadoCelda = celda(fila, '');
        const etiqueta = document.createElement('span');

        etiqueta.className =
            `etiqueta ${clasesEstado[reserva.estado] ?? ''}`;

        etiqueta.textContent =
            nombresEstado[reserva.estado] ?? reserva.estado;

        estadoCelda.appendChild(etiqueta);

        const accionesCelda = celda(fila, '');
        const acciones = document.createElement('div');

        acciones.className = 'acciones';

        acciones.appendChild(
            boton(
                'Huéspedes',
                'accion',
                () => mostrarDetalle(reserva)
            )
        );

        if (['PENDIENTE', 'CONFIRMADA'].includes(reserva.estado)) {
            acciones.appendChild(
                boton(
                    'Cancelar',
                    'accion eliminar',
                    (evento) => cancelarReserva(
                        reserva,
                        evento.currentTarget
                    )
                )
            );
        }

        accionesCelda.appendChild(acciones);
        cuerpo.appendChild(fila);
    });
}

async function cargarReservas() {
    const datos = await peticion(`${API}/listar`);

    if (!Array.isArray(datos)) {
        throw new Error('La API no devolvió una lista de reservas.');
    }

    reservas = [...datos].sort((a, b) => b.id - a.id);
    renderizarReservas();
}

async function cargarHuespedes() {
    const datos = await peticion(`${HUESPEDES_API}/listar`);

    if (!Array.isArray(datos)) {
        throw new Error('La API no devolvió una lista de huéspedes.');
    }

    const selector = $('huespedes');
    selector.replaceChildren();

    datos.forEach((huesped) => {
        const persona = huesped.persona;

        if (!persona) {
            return;
        }

        const opcion = document.createElement('option');

        opcion.value = huesped.id;
        opcion.textContent =
            `${persona.nombre} ${persona.apellido}`
            + ` · ${persona.documento}`;

        selector.appendChild(opcion);
    });

    huespedesCargados = true;
    $('btnNueva').disabled = false;
}

function actualizarResumen() {
    const habitacion = habitacionesDisponibles.get(
        $('habitacion').value
    );

    $('resumenNoches').textContent = habitacion?.noches ?? '—';

    $('resumenPrecio').textContent =
        habitacion ? importe(habitacion.precioPorNoche) : '—';

    $('resumenCapacidad').textContent =
        habitacion?.capacidad ?? '—';

    $('resumenTotal').textContent =
        habitacion ? importe(habitacion.total) : '—';

    const cantidad = $('huespedes').selectedOptions.length;

    $('contadorHuespedes').textContent =
        `${cantidad} huésped(es) seleccionado(s).`
        + (habitacion
            ? ` Capacidad máxima: ${habitacion.capacidad}.`
            : '');

    $('btnGuardar').disabled =
        guardando
        || consultando
        || !fechasConsultadas
        || !habitacion
        || cantidad === 0
        || cantidad > habitacion.capacidad;
}

function invalidarDisponibilidad() {
    numeroConsulta++;
    consultando = false;
    fechasConsultadas = null;

    habitacionesDisponibles.clear();

    $('habitacion').replaceChildren(
        new Option('Consulta disponibilidad primero', '')
    );

    $('habitacion').disabled = true;
    $('btnDisponibilidad').disabled = guardando;
    $('btnDisponibilidad').textContent = 'Consultar disponibilidad';

    $('estadoDisponibilidad').textContent =
        'Selecciona las fechas y consulta disponibilidad.';

    actualizarResumen();
}

function abrirFormulario() {
    formulario.reset();
    $('errorFormulario').hidden = true;

    invalidarDisponibilidad();

    panel.showModal();

    if (!$('huespedes').options.length) {
        mostrarErrorFormulario(
            'Primero registra al menos un huésped '
            + 'desde el módulo Huéspedes.'
        );
    }

    $('fechaEntrada').focus();
}

function cerrarFormulario() {
    if (!guardando) {
        numeroConsulta++;
        panel.close();
    }
}

async function consultarDisponibilidad() {
    $('errorFormulario').hidden = true;

    const entrada = $('fechaEntrada').value;
    const salida = $('fechaSalida').value;

    if (!entrada || !salida || salida <= entrada) {
        mostrarErrorFormulario(
            'Selecciona fechas válidas. '
            + 'La salida debe ser posterior a la entrada.'
        );
        return;
    }

    invalidarDisponibilidad();

    const consultaActual = ++numeroConsulta;

    consultando = true;
    $('btnDisponibilidad').disabled = true;
    $('btnDisponibilidad').textContent = 'Consultando...';

    $('estadoDisponibilidad').textContent =
        'Buscando habitaciones disponibles...';

    actualizarResumen();

    try {
        const parametros = new URLSearchParams({ entrada, salida });

        const datos = await peticion(
            `${API}/disponibles?${parametros}`
        );

        if (consultaActual !== numeroConsulta) {
            return;
        }

        if (!Array.isArray(datos)) {
            throw new Error(
                'La API no devolvió una lista de habitaciones.'
            );
        }

        const selector = $('habitacion');

        selector.replaceChildren(
            new Option(
                datos.length
                    ? 'Selecciona una habitación'
                    : 'No hay habitaciones disponibles',
                ''
            )
        );

        datos.forEach((habitacion) => {
            const id = String(habitacion.id);

            habitacionesDisponibles.set(id, habitacion);

            selector.appendChild(
                new Option(
                    `${habitacion.numero} · ${habitacion.tipo}`
                    + ` · Capacidad: ${habitacion.capacidad}`,
                    id
                )
            );
        });

        fechasConsultadas = { entrada, salida };
        selector.disabled = datos.length === 0;

        $('estadoDisponibilidad').textContent =
            `${datos.length} habitación(es) disponible(s)`
            + ` para ${nochesEntre(entrada, salida)} noche(s).`;
    } catch (error) {
        if (consultaActual === numeroConsulta) {
            mostrarErrorFormulario(error.message);

            $('estadoDisponibilidad').textContent =
                'No se pudo consultar la disponibilidad.';
        }
    } finally {
        if (consultaActual === numeroConsulta) {
            consultando = false;
            $('btnDisponibilidad').disabled = false;

            $('btnDisponibilidad').textContent =
                'Consultar disponibilidad';

            actualizarResumen();
        }
    }
}

function bloquearFormulario(bloqueado) {
    [
        'fechaEntrada',
        'fechaSalida',
        'habitacion',
        'huespedes',
        'estado',
        'btnDisponibilidad'
    ].forEach((id) => {
        $(id).disabled = bloqueado;
    });

    if (!bloqueado) {
        $('habitacion').disabled =
            habitacionesDisponibles.size === 0;
    }

    $('btnGuardar').textContent =
        bloqueado ? 'Registrando...' : 'Registrar reserva';

    actualizarResumen();
}

formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();

    if (guardando) {
        return;
    }

    $('errorFormulario').hidden = true;

    const entrada = $('fechaEntrada').value;
    const salida = $('fechaSalida').value;

    const habitacion = habitacionesDisponibles.get(
        $('habitacion').value
    );

    const huespedIds = Array.from(
        $('huespedes').selectedOptions,
        (opcion) => Number(opcion.value)
    );

    if (!fechasConsultadas
            || fechasConsultadas.entrada !== entrada
            || fechasConsultadas.salida !== salida
            || !habitacion) {
        mostrarErrorFormulario(
            'Consulta disponibilidad y selecciona una habitación.'
        );
        return;
    }

    if (!huespedIds.length) {
        mostrarErrorFormulario('Selecciona al menos un huésped.');
        return;
    }

    if (huespedIds.length > habitacion.capacidad) {
        mostrarErrorFormulario(
            `La habitación admite como máximo `
            + `${habitacion.capacidad} huésped(es).`
        );
        return;
    }

    const datos = {
        habitacionId: habitacion.id,
        fechaEntrada: entrada,
        fechaSalida: salida,
        estado: $('estado').value,
        huespedIds
    };

    guardando = true;
    bloquearFormulario(true);

    try {
        const reserva = await peticion(`${API}/registrar`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(datos)
        });

        panel.close();

        mostrarMensaje(
            `Reserva RES-${reserva.id} registrada correctamente. `
            + `Total: ${importe(reserva.total)}.`
        );

        try {
            await cargarReservas();
        } catch {
            mostrarMensaje(
                'La reserva se registró, pero no se pudo actualizar '
                + 'el listado. Recarga la página.',
                true
            );
        }
    } catch (error) {
        mostrarErrorFormulario(
            `${error.message} Si la solicitud se interrumpió, `
            + 'comprueba el listado antes de intentar registrarla otra vez.'
        );

        // Una reserva de otro usuario pudo cambiar la disponibilidad.
        invalidarDisponibilidad();
    } finally {
        guardando = false;
        bloquearFormulario(false);
    }
});

async function cancelarReserva(reserva, elemento) {
    const confirmado = window.confirm(
        `¿Cancelar la reserva RES-${reserva.id}?\n`
        + 'Se conservarán sus huéspedes y pagos como historial. '
        + 'Esta operación no procesa devoluciones.'
    );

    if (!confirmado) {
        return;
    }

    elemento.disabled = true;

    try {
        await peticion(`${API}/${reserva.id}/cancelar`, {
            method: 'PUT'
        });

        mostrarMensaje(
            `Reserva RES-${reserva.id} cancelada correctamente.`
        );

        try {
            await cargarReservas();
        } catch {
            mostrarMensaje(
                'La cancelación se guardó, pero no se pudo actualizar '
                + 'el listado. Recarga la página.',
                true
            );
        }
    } catch (error) {
        mostrarMensaje(error.message, true);
    } finally {
        elemento.disabled = false;
    }
}

async function mostrarDetalle(reserva) {
    const consultaActual = ++numeroDetalle;

    $('tituloDetalle').textContent = `Reserva RES-${reserva.id}`;
    $('textoDetalle').textContent = 'Cargando huéspedes...';
    $('listaDetalle').replaceChildren();

    const dialogo = $('panelDetalle');

    if (!dialogo.open) {
        dialogo.showModal();
    }

    try {
        const datos = await peticion(
            `${API}/${reserva.id}/huespedes`
        );

        if (consultaActual !== numeroDetalle) {
            return;
        }

        if (!Array.isArray(datos)) {
            throw new Error('No se pudo interpretar el detalle.');
        }

        $('textoDetalle').textContent = datos.length
            ? `${datos.length} huésped(es) asociado(s).`
            : 'Esta reserva no tiene huéspedes asociados.';

        datos.forEach((vinculacion) => {
            const persona = vinculacion.huesped?.persona;
            const tarjeta = document.createElement('article');

            tarjeta.className = 'tarjeta';

            const nombre = document.createElement('strong');

            nombre.textContent = persona
                ? `${persona.nombre} ${persona.apellido}`
                : 'Huésped sin datos personales';

            const documento = document.createElement('p');
            documento.className = 'nota';

            documento.textContent =
                `Documento: ${persona?.documento ?? '—'}`;

            const fechas = document.createElement('p');
            fechas.className = 'nota';

            fechas.textContent =
                `${fechaVisible(vinculacion.fechaEntrada)}`
                + ` — ${fechaVisible(vinculacion.fechaSalida)}`;

            tarjeta.append(nombre, documento, fechas);
            $('listaDetalle').appendChild(tarjeta);
        });
    } catch (error) {
        if (consultaActual === numeroDetalle) {
            $('textoDetalle').textContent = error.message;
        }
    }
}

$('btnNueva').addEventListener('click', abrirFormulario);
$('btnCerrar').addEventListener('click', cerrarFormulario);
$('btnCancelar').addEventListener('click', cerrarFormulario);

$('btnDisponibilidad').addEventListener(
    'click',
    consultarDisponibilidad
);

$('fechaEntrada').addEventListener(
    'input',
    invalidarDisponibilidad
);

$('fechaSalida').addEventListener(
    'input',
    invalidarDisponibilidad
);

$('habitacion').addEventListener('change', actualizarResumen);
$('huespedes').addEventListener('change', actualizarResumen);

$('buscar').addEventListener('input', renderizarReservas);
$('filtroEstado').addEventListener('change', renderizarReservas);

$('btnCerrarDetalle').addEventListener('click', () => {
    numeroDetalle++;
    $('panelDetalle').close();
});

$('panelDetalle').addEventListener('cancel', () => {
    numeroDetalle++;
});

panel.addEventListener('cancel', (evento) => {
    if (guardando) {
        evento.preventDefault();
    } else {
        numeroConsulta++;
    }
});

async function iniciar() {
    const resultados = await Promise.allSettled([
        cargarReservas(),
        cargarHuespedes()
    ]);

    const errores = [];

    if (resultados[0].status === 'rejected') {
        filaInformativa('No se pudieron cargar las reservas.');
        errores.push(resultados[0].reason.message);
    }

    if (resultados[1].status === 'rejected') {
        errores.push(
            'No se pudieron cargar los huéspedes. '
            + 'Recarga la página antes de crear una reserva. '
            + resultados[1].reason.message
        );
    }

    if (errores.length) {
        mostrarMensaje(errores.join(' '), true);
    }

    $('btnNueva').disabled = !huespedesCargados;
}

iniciar();