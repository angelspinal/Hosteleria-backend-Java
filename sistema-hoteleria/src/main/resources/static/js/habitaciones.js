'use strict';

const $ = (id) => document.getElementById(id);

const API = document.body.dataset.apiBase;

const panel = $('panelHabitacion');
const formulario = $('formHabitacion');
const botonGuardar = $('btnGuardar');

const etiquetas = {
    HABILITADA: 'Habilitada',
    MANTENIMIENTO: 'Mantenimiento',
    INACTIVA: 'Inactiva'
};

const formatoNumero = new Intl.NumberFormat('es', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
});

let habitaciones = [];
let idEdicion = null;

function mostrarMensaje(texto, error = false) {
    const elemento = $('mensaje');

    elemento.textContent = texto;
    elemento.className = `mensaje ${error ? 'error' : 'exito'}`;
    elemento.hidden = false;
}

function mostrarErrorFormulario(texto) {
    $('errorFormulario').textContent = texto;
    $('errorFormulario').hidden = false;
}

async function peticion(ruta, opciones = {}) {
    const respuesta = await fetch(`${API}${ruta}`, opciones);
    const texto = await respuesta.text();

    let datos = texto;

    if (texto) {
        try {
            datos = JSON.parse(texto);
        } catch {
            // La API también devuelve errores como texto.
        }
    }

    if (!respuesta.ok) {
        let mensaje = `No se pudo completar la operación (${respuesta.status}).`;

        if (typeof datos === 'string' && datos.trim()) {
            mensaje = datos.trim().startsWith('<')
                ? mensaje
                : datos;
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

function actualizarResumen() {
    $('total').textContent = habitaciones.length;

    $('habilitadas').textContent = habitaciones.filter(
        (h) => h.estado === 'HABILITADA'
    ).length;

    $('mantenimiento').textContent = habitaciones.filter(
        (h) => h.estado === 'MANTENIMIENTO'
    ).length;

    $('inactivas').textContent = habitaciones.filter(
        (h) => h.estado === 'INACTIVA'
    ).length;
}

function crearCelda(fila, texto, clase = '') {
    const celda = document.createElement('td');

    celda.textContent = texto;
    celda.className = clase;

    fila.appendChild(celda);

    return celda;
}

function crearBoton(texto, clase, accion) {
    const boton = document.createElement('button');

    boton.type = 'button';
    boton.textContent = texto;
    boton.className = clase;
    boton.addEventListener('click', accion);

    return boton;
}

function renderizarTabla() {
    const busqueda = $('buscar').value.trim().toLocaleLowerCase('es');
    const estado = $('filtroEstado').value;

    const filtradas = habitaciones.filter((h) => {
        const texto = [
            h.numero,
            h.tipoHabitacion?.nombre,
            h.piso
        ].join(' ').toLocaleLowerCase('es');

        return texto.includes(busqueda)
            && (!estado || h.estado === estado);
    });

    const cuerpo = $('tablaHabitaciones');
    cuerpo.replaceChildren();

    $('cantidadResultados').textContent =
        `${filtradas.length} de ${habitaciones.length} habitaciones`;

    if (!filtradas.length) {
        const fila = document.createElement('tr');
        const celda = crearCelda(
            fila,
            habitaciones.length
                ? 'No hay resultados para estos filtros.'
                : 'Todavía no hay habitaciones registradas.',
            'tabla-vacia'
        );

        celda.colSpan = 7;
        cuerpo.appendChild(fila);
        return;
    }

    filtradas.forEach((habitacion) => {
        const fila = document.createElement('tr');
        const tipo = habitacion.tipoHabitacion;

        crearCelda(fila, habitacion.numero, 'numero-habitacion');
        crearCelda(fila, tipo?.nombre ?? '—');
        crearCelda(fila, habitacion.piso);

        crearCelda(
            fila,
            tipo?.precioBase != null
                ? formatoNumero.format(tipo.precioBase)
                : '—'
        );

        crearCelda(fila, tipo?.capacidad ?? '—');

        const celdaEstado = crearCelda(fila, '');
        const etiqueta = document.createElement('span');

        etiqueta.textContent =
            etiquetas[habitacion.estado] ?? habitacion.estado;

        etiqueta.className = 'etiqueta';

        const clasesEstado = {
            HABILITADA: 'habilitada',
            MANTENIMIENTO: 'mantenimiento',
            INACTIVA: 'inactiva'
        };

        if (clasesEstado[habitacion.estado]) {
            etiqueta.classList.add(clasesEstado[habitacion.estado]);
        }

        celdaEstado.appendChild(etiqueta);

        const acciones = crearCelda(fila, '');
        const grupo = document.createElement('div');

        grupo.className = 'acciones';

        grupo.append(
            crearBoton(
                'Editar',
                'accion',
                () => abrirFormulario(habitacion)
            ),
            crearBoton(
                'Eliminar',
                'accion eliminar',
                (evento) => eliminarHabitacion(
                    habitacion,
                    evento.currentTarget
                )
            )
        );

        acciones.appendChild(grupo);
        cuerpo.appendChild(fila);
    });
}

async function cargarHabitaciones() {
    const datos = await peticion('/listar');

    if (!Array.isArray(datos)) {
        throw new Error('La API no devolvió una lista de habitaciones.');
    }

    habitaciones = datos;
    actualizarResumen();
    renderizarTabla();
}

function actualizarDatosTipo() {
    const opcion = $('tipoHabitacion').selectedOptions[0];

    const precio = opcion?.dataset.precio;
    const capacidad = opcion?.dataset.capacidad;

    $('precioTipo').textContent =
        precio ? formatoNumero.format(Number(precio)) : '—';

    $('capacidadTipo').textContent =
        capacidad ? `${capacidad} persona(s)` : '—';
}

function abrirFormulario(habitacion = null) {
    formulario.reset();
    $('errorFormulario').hidden = true;

    idEdicion = habitacion?.id ?? null;

    $('tituloPanel').textContent =
        idEdicion !== null ? 'Editar habitación' : 'Nueva habitación';

    $('btnGuardar').textContent =
        idEdicion !== null ? 'Guardar cambios' : 'Crear habitación';

    if (habitacion) {
        $('numero').value = habitacion.numero;
        $('piso').value = habitacion.piso;
        $('estado').value = habitacion.estado;

        $('tipoHabitacion').value =
            habitacion.tipoHabitacion?.id ?? '';
    } else {
        $('estado').value = 'HABILITADA';
    }

    actualizarDatosTipo();
    panel.showModal();
    $('numero').focus();
}

function cerrarFormulario() {
    if (!botonGuardar.disabled) {
        panel.close();
    }
}

formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();

    $('errorFormulario').hidden = true;

    const numero = $('numero').value.trim();
    const piso = Number($('piso').value);
    const tipoId = Number($('tipoHabitacion').value);

    if (!numero) {
        mostrarErrorFormulario('El número de habitación es obligatorio.');
        $('numero').focus();
        return;
    }

    if (!Number.isInteger(piso)) {
        mostrarErrorFormulario('El piso debe ser un número entero.');
        return;
    }

    if (!tipoId) {
        mostrarErrorFormulario('Selecciona un tipo de habitación.');
        return;
    }

    const datos = {
        numero,
        piso,
        estado: $('estado').value,
        tipoHabitacion: {
            id: tipoId
        }
    };

    const editando = idEdicion !== null;

    const ruta = editando
        ? `/actualizar/${idEdicion}`
        : '/crear';

    const textoBoton = botonGuardar.textContent;

    botonGuardar.disabled = true;
    botonGuardar.textContent = 'Guardando...';

    try {
        await peticion(ruta, {
            method: editando ? 'PUT' : 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(datos)
        });

        panel.close();

        mostrarMensaje(
            editando
                ? 'Habitación actualizada correctamente.'
                : 'Habitación creada correctamente.'
        );

        try {
            await cargarHabitaciones();
        } catch {
            mostrarMensaje(
                'Los cambios se guardaron, pero no se pudo actualizar '
                + 'la lista. Recarga la página.',
                true
            );
        }
    } catch (error) {
        mostrarErrorFormulario(error.message);
    } finally {
        botonGuardar.disabled = false;
        botonGuardar.textContent = textoBoton;
    }
});

async function eliminarHabitacion(habitacion, boton) {
    const confirmado = window.confirm(
        `¿Eliminar la habitación ${habitacion.numero}?\n`
        + 'Esta acción no se puede deshacer.'
    );

    if (!confirmado) {
        return;
    }

    boton.disabled = true;

    try {
        await peticion(`/eliminar/${habitacion.id}`, {
            method: 'DELETE'
        });

        mostrarMensaje('Habitación eliminada correctamente.');

        try {
            await cargarHabitaciones();
        } catch {
            mostrarMensaje(
                'La habitación se eliminó, pero no se pudo actualizar '
                + 'la lista. Recarga la página.',
                true
            );
        }
    } catch (error) {
        mostrarMensaje(error.message, true);
    } finally {
        boton.disabled = false;
    }
}

$('btnNueva').addEventListener('click', () => abrirFormulario());
$('btnCerrar').addEventListener('click', cerrarFormulario);
$('btnCancelar').addEventListener('click', cerrarFormulario);

$('buscar').addEventListener('input', renderizarTabla);
$('filtroEstado').addEventListener('change', renderizarTabla);
$('tipoHabitacion').addEventListener('change', actualizarDatosTipo);

panel.addEventListener('cancel', (evento) => {
    if (botonGuardar.disabled) {
        evento.preventDefault();
    }
});

cargarHabitaciones().catch((error) => {
    const cuerpo = $('tablaHabitaciones');
    cuerpo.replaceChildren();

    const fila = document.createElement('tr');
    const celda = crearCelda(
        fila,
        'No se pudieron cargar las habitaciones.',
        'tabla-vacia'
    );

    celda.colSpan = 7;
    cuerpo.appendChild(fila);

    mostrarMensaje(error.message, true);
});