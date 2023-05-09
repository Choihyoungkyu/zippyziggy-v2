import { useCallback, useEffect, useState } from 'react';
import {
  ZP_INPUT_WRAPPER_ID,
  ZP_PROMPT_CONTAINER_ID,
  ZP_PROMPT_TITLE_HOLDER_ID,
} from '@pages/constants';
import {
  addToggleButton,
  addToTopButton,
  adjustToBottomButtonPosition,
  createPortalContainer,
  removeFormParentClasses,
  setInputWrapperStyle,
} from '@pages/content/utils/extension/add-ui-to-input-portals';
import {
  findRegenerateButton,
  hideEmptyDiv,
} from '@pages/content/utils/extension/add-ui-to-prompt-portals';

const useInputContainerPortal = () => {
  const [portalContainer, setPortalContainer] = useState<HTMLDivElement | null>(null);

  const addInputWrapperPortal = useCallback(() => {
    const existingPortal = document.getElementById(ZP_INPUT_WRAPPER_ID);
    if (existingPortal) return;

    const $formParent = document.querySelector('form')?.parentElement;
    if (!$formParent) return;

    // 클래스 제거
    removeFormParentClasses($formParent);

    // 인풋 포탈을 생성
    const $inputWrapperPortal = createPortalContainer();
    if (!$inputWrapperPortal) return;

    // 토글 버튼을 생성 후 주입
    addToggleButton($formParent);
    // 입력창 focus 시 border 스타일 지정
    setInputWrapperStyle($inputWrapperPortal.parentElement);
    const $selectedPromptTitle = document.createElement('p');
    $selectedPromptTitle.id = ZP_PROMPT_TITLE_HOLDER_ID;

    $inputWrapperPortal.prepend($selectedPromptTitle);

    setPortalContainer($inputWrapperPortal);
  }, []);

  useEffect(() => {
    const shouldCreateInputWrapperPortal = (element: Element): boolean => {
      const { id, className } = element;

      const isPromptContainer = id === ZP_PROMPT_CONTAINER_ID;
      const isRoot = id === '__next';
      const isInputWrapper = className?.includes(
        'flex ml-1 md:w-full md:m-auto md:mb-2 gap-0 md:gap-2 justify-center'
      );
      const isChange = className?.includes('overflow-hidden w-full h-full relative flex');

      return Boolean(isPromptContainer || isRoot || isInputWrapper || isChange);
    };

    // MutationObserver를 이용하여 __next 요소의 자식요소 추가, 제거, 변경을 감지하고,
    // 해당되는 경우 포탈을 생성하도록 addPromptContainerPortal 함수를 호출함
    const observer = new MutationObserver((mutations) => {
      for (const mutation of mutations) {
        const targetElement = mutation.target as Element;
        if (
          (targetElement.id === ZP_INPUT_WRAPPER_ID &&
            document.querySelector("[class^='react-scroll-to-bottom--css']")?.children[0]) ||
          (targetElement.className === 'ZP_prompt-container__inner' &&
            document.querySelector("[class^='react-scroll-to-bottom--css']")?.children[0]) ||
          (targetElement.className === 'flex-1 overflow-hidden' &&
            document.querySelector("[class^='react-scroll-to-bottom--css']")?.children[0])
        ) {
          const scrollWrapper = document.querySelector("[class^='react-scroll-to-bottom--css']")
            ?.children[0];
          if (scrollWrapper.scrollHeight > scrollWrapper.clientHeight) {
            addToTopButton(scrollWrapper);
          }
        }

        if (targetElement.id === ZP_INPUT_WRAPPER_ID) {
          const $ZPActionGroup = document.querySelector('#ZP_actionGroup');
          if (!$ZPActionGroup) return;
          const isNewChatPage = !window.location.href.includes('/c/');
          if (!isNewChatPage) $ZPActionGroup.classList.remove('ZP_invisible');
          if (findRegenerateButton()) $ZPActionGroup.classList.remove('ZP_invisible');
        }

        if (targetElement.className.includes('react-scroll-to-bottom--css')) {
          // GPT 사이트의 맨 아래로 가는 버튼의 위치를 조정
          adjustToBottomButtonPosition(targetElement as HTMLElement);
        }

        if (shouldCreateInputWrapperPortal(targetElement)) {
          addInputWrapperPortal();
          return;
        }

        hideEmptyDiv(targetElement);
      }
    });

    // observer를 __next 요소에 observe하도록 설정
    const nextJSElement = document.getElementById('__next');
    if (nextJSElement) observer.observe(nextJSElement, { subtree: true, childList: true });

    // cleanup 함수에서 observer를 disconnect하도록 설정
    return () => {
      observer.disconnect();
    };
  }, [addInputWrapperPortal]);

  return portalContainer;
};

export default useInputContainerPortal;
