import React, { useEffect, useRef, useState } from 'react';
import { getForkListUsePrompt } from '@/core/prompt/promptAPI';
import { getPromptsMemberAPI } from '@/core/user/userAPI';
import { CardList, Container } from './ComponentStyle';
import PromptCard from '../PromptCard/PromptCard';
import Paging from '../Paging/Paging';
import Paragraph from '../Typography/Paragraph';

type PropsType = {
  userUuid: string | string[] | number;
  size: number;
  className?: string;
  getData: (data: any) => any;
};

export default function ProfilePromptList({ className, userUuid, size = 6, getData }: PropsType) {
  const [cardList, setCardList] = useState<Array<any>>([]);
  const [totalPromptsCnt, setTotalPromptsCnt] = useState<number>(0);

  const page = useRef<number>(0);

  // User가 올린 Prompt 가져오기
  const handlePromptsMember = async () => {
    const requestData = {
      id: userUuid,
      page: page.current,
      size,
    };
    try {
      const data = await getData(requestData);
      if (data.result === 'SUCCESS') {
        setCardList(data.data.promptCardResponseList);
        setTotalPromptsCnt(data.data.totalPromptsCnt);
      }
    } catch (err) {
      err;
    }
  };

  useEffect(() => {
    if (userUuid) {
      handlePromptsMember();
      return () => {
        page.current = 0;
      };
    }
    return () => {};
  }, [userUuid]);

  // 페이지 이동
  const handlePage = (number: number) => {
    page.current = number - 1;
    handlePromptsMember();
  };

  return (
    <Container className={className}>
      {cardList.length === 0 && (
        <Paragraph textAlign="center" style={{ padding: '16px' }}>
          비어있어요!
        </Paragraph>
      )}
      <CardList>
        {cardList.length > 0
          ? cardList?.map((prompt: any) => (
              <PromptCard
                key={prompt.promptUuid}
                prompt={prompt}
                url={`/prompts/${prompt.promptUuid}`}
              />
            ))
          : null}
      </CardList>
      <Paging
        page={page.current}
        size={size}
        totalCnt={totalPromptsCnt || 0}
        setPage={handlePage}
      />
    </Container>
  );
}
