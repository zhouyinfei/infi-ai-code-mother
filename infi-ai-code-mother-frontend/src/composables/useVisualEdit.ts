import { ref, onBeforeUnmount, watch, type Ref } from 'vue'

export interface SelectedElement {
  _uid: string
  tagName: string
  className: string
  id: string
  textContent: string
  selector: string
}

export function useVisualEdit(
  iframeRef: Ref<HTMLIFrameElement | null>,
  previewUrl: Ref<string>,
) {
  const editMode = ref(false)
  const selectedElements = ref<SelectedElement[]>([])

  const injectVisualEditScript = (iframe: HTMLIFrameElement): boolean => {
    try {
      const doc = iframe.contentDocument
      if (!doc) return false

      const existingScript = doc.getElementById('__visual_edit_script__')
      if (existingScript) {
        const win = iframe.contentWindow as any
        if (win.__cleanupVisualEdit__) {
          win.__cleanupVisualEdit__()
        }
        existingScript.remove()
      }

      const script = doc.createElement('script')
      script.id = '__visual_edit_script__'
      script.textContent = `
(function() {
  if (window.__visualEditCleanup__) {
    window.__visualEditCleanup__();
  }

  const HOVER_STYLE = 'outline: 2px dashed #1677ff !important; outline-offset: 2px !important; cursor: pointer !important;';
  const SELECTED_STYLE = 'outline: 3px solid #0958d9 !important; outline-offset: 2px !important; background: rgba(22, 119, 255, 0.08) !important; cursor: pointer !important;';
  const ATTR = 'data-ve-uid';

  let selectedElements = [];
  let uidCounter = 0;

  function getSelector(el) {
    if (el.id) return '#' + el.id;
    let path = el.tagName.toLowerCase();
    if (el.className && typeof el.className === 'string') {
      path += '.' + el.className.trim().split(/\\s+/).join('.');
    }
    return path;
  }

  function getElementInfo(el) {
    let uid = el.getAttribute(ATTR);
    if (!uid) {
      uid = 've_' + (++uidCounter) + '_' + Date.now();
      el.setAttribute(ATTR, uid);
    }
    return {
      _uid: uid,
      tagName: el.tagName.toLowerCase(),
      className: (typeof el.className === 'string' ? el.className : '').trim(),
      id: el.id || '',
      textContent: (el.textContent || '').trim().substring(0, 100),
      selector: getSelector(el)
    };
  }

  function notifyParent() {
    window.parent.postMessage({
      type: 'visual-edit:elements-selected',
      elements: selectedElements
    }, '*');
  }

  function addSelectedClass(el) {
    el.setAttribute(ATTR + '-selected', 'true');
  }

  function removeSelectedClass(el) {
    el.removeAttribute(ATTR + '-selected');
  }

  function onMouseOver(e) {
    if (!window.__visualEditMode__) return;
    const target = e.target;
    if (!target || target === document.body || target === document.documentElement) return;
    target.setAttribute(ATTR + '-hover', 'true');
    target.setAttribute('style', (target.getAttribute('style') || '') + ';' + HOVER_STYLE);
  }

  function onMouseOut(e) {
    const target = e.target;
    if (!target) return;
    target.removeAttribute(ATTR + '-hover');
    let style = target.getAttribute('style') || '';
    style = style.replace(/outline:[^;]*!important;\\s*/g, '')
                 .replace(/outline-offset:[^;]*!important;\\s*/g, '')
                 .replace(/cursor:[^;]*!important;\\s*/g, '')
                 .replace(/background:[^;]*!important;\\s*/g, '');
    if (style.trim() === ';') style = '';
    target.setAttribute('style', style);
  }

  function onMouseDown(e) {
    if (!window.__visualEditMode__) return;
    const target = e.target;
    if (!target || target === document.body || target === document.documentElement) return;

    e.preventDefault();
    e.stopPropagation();

    const uid = target.getAttribute(ATTR) || ('ve_' + (++uidCounter) + '_' + Date.now());
    target.setAttribute(ATTR, uid);

    const existingIndex = selectedElements.findIndex(el => el._uid === uid);
    if (existingIndex !== -1) {
      selectedElements.splice(existingIndex, 1);
      removeSelectedClass(target);
    } else {
      const info = getElementInfo(target);
      selectedElements.push(info);
      addSelectedClass(target);
      target.setAttribute('style', (target.getAttribute('style') || '') + ';' + SELECTED_STYLE);
    }

    notifyParent();
  }

  document.addEventListener('mouseover', onMouseOver, true);
  document.addEventListener('mouseout', onMouseOut, true);
  document.addEventListener('mousedown', onMouseDown, true);

  window.__visualEditMode__ = true;

  window.__visualEditCleanup__ = function() {
    document.removeEventListener('mouseover', onMouseOver, true);
    document.removeEventListener('mouseout', onMouseOut, true);
    document.removeEventListener('mousedown', onMouseDown, true);

    document.querySelectorAll('[' + ATTR + '-hover]').forEach(el => {
      el.removeAttribute(ATTR + '-hover');
    });

    document.querySelectorAll('[' + ATTR + '-selected]').forEach(el => {
      el.removeAttribute(ATTR + '-selected');
      let style = el.getAttribute('style') || '';
      style = style.replace(/outline:[^;]*!important;\\s*/g, '')
                   .replace(/outline-offset:[^;]*!important;\\s*/g, '')
                   .replace(/cursor:[^;]*!important;\\s*/g, '')
                   .replace(/background:[^;]*!important;\\s*/g, '');
      if (style.trim() === ';') style = '';
      el.setAttribute('style', style);
    });

    window.__visualEditMode__ = false;
  };
})();
      `

      doc.head?.appendChild(script)
      return true
    } catch (error) {
      console.error('[useVisualEdit] Failed to inject script:', error)
      return false
    }
  }

  const enterVisualEdit = () => {
    editMode.value = true
    const iframe = iframeRef.value
    if (iframe) {
      const success = injectVisualEditScript(iframe)
      if (!success) {
        iframe.onload = () => {
          if (editMode.value) {
            injectVisualEditScript(iframe)
          }
        }
      }
    }
  }

  const exitVisualEdit = () => {
    editMode.value = false
    selectedElements.value = []

    const iframe = iframeRef.value
    if (iframe) {
      try {
        const doc = iframe.contentDocument
        if (doc) {
          const win = iframe.contentWindow as any
          if (win.__visualEditCleanup__) {
            win.__visualEditCleanup__()
            win.__visualEditCleanup__ = null
          }
          const script = doc.getElementById('__visual_edit_script__')
          if (script) script.remove()
        }
      } catch (error) {
        console.error('[useVisualEdit] Failed to cleanup:', error)
      }
    }
  }

  const onMessage = (event: MessageEvent) => {
    if (event.data?.type === 'visual-edit:elements-selected') {
      selectedElements.value = event.data.elements || []
    }
  }

  window.addEventListener('message', onMessage)

  watch(iframeRef, (newIframe) => {
    if (newIframe && editMode.value) {
      newIframe.onload = () => {
        if (editMode.value) {
          injectVisualEditScript(newIframe)
        }
      }
    }
  })

  watch(previewUrl, () => {
    const iframe = iframeRef.value
    if (iframe && editMode.value) {
      iframe.onload = () => {
        if (editMode.value) {
          injectVisualEditScript(iframe)
        }
      }
    }
  })

  onBeforeUnmount(() => {
    exitVisualEdit()
    window.removeEventListener('message', onMessage)
  })

  return {
    editMode,
    selectedElements,
    enterVisualEdit,
    exitVisualEdit,
  }
}
