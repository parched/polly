import { useEffect, useState } from 'react';

interface AsyncResult<R> { result: R | undefined, error: any, isPending: boolean }

export function useAsyncFn<Args, Result>(promiseFn: (args: Args) => Promise<Result>, args: Args): AsyncResult<Result> {
    const [ret, setRet] = useState<AsyncResult<Result>>({ result: undefined, error: undefined, isPending: true });

    useEffect(() => {
        let unmounted = false;

        promiseFn(args).then(
            result => {
                if (!unmounted) {
                    setRet({ result: result, error: undefined, isPending: false });
                }
            },
            error => {
                if (!unmounted) {
                    setRet({ result: undefined, error: error, isPending: false });
                }
            }
        )
        return () => { unmounted = true; };
    }, [promiseFn, args])

    return ret;
}